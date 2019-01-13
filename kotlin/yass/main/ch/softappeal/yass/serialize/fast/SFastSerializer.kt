package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.*
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.serialize.fast.SObjectType.*
import java.lang.reflect.*
import java.util.*
import kotlin.reflect.*

abstract class STypeSerializer internal constructor(val type: Class<*>, private val objectType: SObjectType) {
    internal abstract suspend fun read(input: SFastSerializer.Input): Any?
    internal abstract suspend fun write(output: SFastSerializer.Output, value: Any?)
    internal open suspend fun write(output: SFastSerializer.Output, id: Int, value: Any?) {
        output.writer.writeVarInt(if (output.skipping) objectType.skippingId(id) else id)
        write(output, value)
    }
}

class STypeDesc(val id: Int, val serializer: STypeSerializer) {
    init {
        require(id >= 0) { "id $id for type '${serializer.type.canonicalName}' must be >= 0" }
        require(id <= MaxTypeId) { "id $id for type '${serializer.type.canonicalName}' must be <= $MaxTypeId" }
    }

    internal suspend fun write(output: SFastSerializer.Output, value: Any?) = serializer.write(output, id, value)
}

val SNullTypeDesc = STypeDesc(0, object : STypeSerializer(VoidType::class.java, TreeClass) {
    override suspend fun read(input: SFastSerializer.Input): Any? = null
    override suspend fun write(output: SFastSerializer.Output, value: Any?) {}
})

val SReferenceTypeDesc = STypeDesc(1, object : STypeSerializer(ReferenceType::class.java, TreeClass) {
    override suspend fun read(input: SFastSerializer.Input) = input.objects!![input.reader.readVarInt()]
    override suspend fun write(output: SFastSerializer.Output, value: Any?) = output.writer.writeVarInt(value as Int)
})

val SListTypeDesc = STypeDesc(2, object : STypeSerializer(List::class.java, TreeClass) {
    override suspend fun read(input: SFastSerializer.Input): List<*> {
        var length = input.reader.readVarInt()
        val list = mutableListOf<Any?>()
        while (length-- > 0) list.add(input.read())
        return list
    }

    override suspend fun write(output: SFastSerializer.Output, value: Any?) {
        val list = value as List<*>
        output.writer.writeVarInt(list.size)
        for (e in list) output.write(e)
    }
})

abstract class SBaseTypeSerializer<V : Any> protected constructor(
    type: Class<V>, val fieldType: SFieldType
) : STypeSerializer(type, fieldType.objectType) {
    protected constructor(type: KClass<V>, fieldType: SFieldType) : this(type.javaObjectType, fieldType)

    init {
        check(fieldType != SFieldType.Class && fieldType != SFieldType.List)
    }

    final override suspend fun read(input: SFastSerializer.Input) = read(input.reader)
    @Suppress("UNCHECKED_CAST")
    final override suspend fun write(output: SFastSerializer.Output, value: Any?) = write(output.writer, value as V)

    abstract suspend fun write(writer: SWriter, value: V)
    abstract suspend fun read(reader: SReader): V
}

class SFieldDesc internal constructor(val id: Int, val serializer: SFastSerializer.FieldSerializer)

abstract class SFastSerializer protected constructor(val skipping: Boolean) : SSerializer {
    private val class2typeDesc = HashMap<Class<*>, STypeDesc>(64)
    private val _id2typeSerializer = HashMap<Int, STypeSerializer>(64)

    init {
        addType(SNullTypeDesc)
        addType(SReferenceTypeDesc)
        addType(SListTypeDesc)
    }

    val id2typeSerializer: Map<Int, STypeSerializer> get() = TreeMap(_id2typeSerializer)

    private fun addType(typeDesc: STypeDesc) {
        require(class2typeDesc.put(typeDesc.serializer.type, typeDesc) == null) {
            "type '${typeDesc.serializer.type.canonicalName}' already added"
        }
        val oldTypeSerializer = _id2typeSerializer.put(typeDesc.id, typeDesc.serializer)
        check(oldTypeSerializer == null) {
            "type id ${typeDesc.id} used for '${typeDesc.serializer.type.canonicalName}' " +
                "and '${oldTypeSerializer!!.type.canonicalName}'"
        }
    }

    protected fun addEnum(id: Int, type: KClass<*>) {
        require(type.java.isEnum) { "type '${type.java.canonicalName}' is not an enumeration" }
        @Suppress("UNCHECKED_CAST") val enumeration = type.java as Class<Enum<*>>
        val constants = enumeration.enumConstants
        addType(STypeDesc(id, object : SBaseTypeSerializer<Enum<*>>(enumeration, SFieldType.VarInt) {
            override suspend fun read(reader: SReader) = constants[reader.readVarInt()]
            override suspend fun write(writer: SWriter, value: Enum<*>) = writer.writeVarInt(value.ordinal)
        }))
    }

    protected fun checkClass(type: KClass<*>) =
        require(!type.java.isEnum) { "type '${type.java.canonicalName}' is an enumeration" }

    inner class ClassTypeSerializer internal constructor(
        type: Class<*>, val graph: Boolean, private val id2fieldSerializer: Map<Int, FieldSerializer>
    ) : STypeSerializer(type, if (graph) GraphClass else TreeClass) {
        private val allocator = AllocatorFactory(type)
        val fieldDescs: List<SFieldDesc>

        init {
            val fds = mutableListOf<SFieldDesc>()
            for ((id, serializer) in id2fieldSerializer) {
                require(id >= FirstFieldId) { "id $id for field '${serializer.field}' must be >= $FirstFieldId" }
                require(id <= MaxFieldId) { "id $id for field '${serializer.field}' must be <= $MaxFieldId" }
                fds.add(SFieldDesc(id, serializer))
            }
            fieldDescs = Collections.unmodifiableList(fds.sortedBy { it.id })
        }

        internal fun fixupFields() {
            for ((id, serializer) in id2fieldSerializer) serializer.fixup(id)
        }

        override suspend fun read(input: Input): Any {
            val value = allocator()
            if (graph) input.addToObjects(value)
            while (true) {
                val skippingId = input.reader.readVarInt()
                val id = if (skipping) fieldIdFromSkippingId(skippingId) else skippingId
                if (id == EndFieldId) return value
                val fieldSerializer = id2fieldSerializer[id]
                if (fieldSerializer == null) {
                    if (skipping) sFieldTypeFromSkippingId(skippingId).skip(input)
                    else error("class '${type.canonicalName}' doesn't have a field with id $id")
                } else fieldSerializer.read(input, value)
            }
        }

        override suspend fun write(output: Output, id: Int, value: Any?) {
            if (graph) {
                if (output.object2reference == null) output.object2reference = IdentityHashMap(16)
                val object2reference = output.object2reference
                val reference = object2reference!![value]
                if (reference != null) {
                    SReferenceTypeDesc.write(output, reference)
                    return
                }
                object2reference[value!!] = object2reference.size
            }
            super.write(output, id, value)
        }

        override suspend fun write(output: Output, value: Any?) {
            for (fieldDesc in fieldDescs) fieldDesc.serializer.write(output, value!!)
            output.writer.writeVarInt(EndFieldId)
        }
    }

    inner class FieldSerializer internal constructor(val field: Field) {
        private var _typeSerializer: STypeSerializer? = null
        val typeSerializer: STypeSerializer? get() = _typeSerializer

        private var id: Int = 0

        internal fun fixup(fieldId: Int) {
            val typeDesc = class2typeDesc[primitiveWrapperType(field.type)]
            _typeSerializer = typeDesc?.serializer
            if (_typeSerializer is ClassTypeSerializer) _typeSerializer = null
            id = if (!skipping) fieldId else when (_typeSerializer) {
                null -> SFieldType.Class
                SListTypeDesc.serializer -> SFieldType.List
                else -> (_typeSerializer as SBaseTypeSerializer<*>).fieldType
            }.skippingId(fieldId)
        }

        internal suspend fun read(input: Input, value: Any) =
            field.set(value, if (_typeSerializer == null) input.read() else _typeSerializer!!.read(input))

        internal suspend fun write(output: Output, value: Any) {
            val f = field.get(value)
            if (f != null) {
                output.writer.writeVarInt(id)
                if (_typeSerializer == null) output.write(f) else _typeSerializer!!.write(output, f)
            }
        }
    }

    protected fun addClass(id: Int, type: KClass<*>, graph: Boolean, id2field: Map<Int, Field>) {
        require(!Modifier.isAbstract(type.java.modifiers)) { "type '${type.java.canonicalName}' is abstract" }
        val id2fieldSerializer = mutableMapOf<Int, FieldSerializer>()
        val name2field = mutableMapOf<String, Field>()
        for ((fieldId, field) in id2field) {
            val oldField = name2field.put(field.name, field)
            require(oldField == null) {
                "duplicated field name '$field' and '$oldField' not allowed in class hierarchy"
            }
            id2fieldSerializer[fieldId] = FieldSerializer(field)
        }
        addType(STypeDesc(id, ClassTypeSerializer(type.java, graph, id2fieldSerializer)))
    }

    protected fun addBaseType(typeDesc: STypeDesc) {
        require(!typeDesc.serializer.type.isEnum) {
            "base type '${typeDesc.serializer.type.canonicalName}' is an enumeration"
        }
        addType(typeDesc)
    }

    private fun checkParentClasses() {
        _id2typeSerializer.forEach { id, typeSerializer ->
            if (id < FirstTypeId) return@forEach
            if (typeSerializer is ClassTypeSerializer) {
                var t = typeSerializer.type
                while (!isRootClass(t)) {
                    if (!Modifier.isAbstract(t.modifiers))
                        require(class2typeDesc.contains(t)) { "missing base class '${t.canonicalName}'" }
                    t = t.superclass
                }
            }
        }
    }

    protected fun fixup() {
        for (typeDesc in class2typeDesc.values) (typeDesc.serializer as? ClassTypeSerializer)?.fixupFields()
        checkParentClasses()
    }

    internal inner class Input(val reader: SReader) {
        var objects: MutableList<Any>? = null
        fun addToObjects(value: Any) {
            if (objects == null) objects = mutableListOf()
            objects!!.add(value)
        }

        suspend fun read(): Any? {
            val skippingId = reader.readVarInt()
            val id = if (skipping) typeIdFromSkippingId(skippingId) else skippingId
            return (_id2typeSerializer[id] ?: error("no type with id $id")).read(this)
        }
    }

    internal inner class Output(val writer: SWriter) {
        val skipping = this@SFastSerializer.skipping
        var object2reference: MutableMap<Any, Int>? = null
        suspend fun write(value: Any?): Unit = when (value) {
            null -> SNullTypeDesc.write(this, null)
            is List<*> -> SListTypeDesc.write(this, value)
            else -> (class2typeDesc[value.javaClass] ?: error("missing type '${value.javaClass.canonicalName}'"))
                .write(this, value)
        }
    }

    override suspend fun read(reader: SReader) = Input(reader).read()
    override suspend fun write(writer: SWriter, value: Any?) = Output(writer).write(value)
}
