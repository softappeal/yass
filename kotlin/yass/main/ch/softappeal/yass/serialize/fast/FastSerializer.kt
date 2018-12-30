package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.*
import ch.softappeal.yass.serialize.*
import java.lang.reflect.*
import java.util.*

internal class Input(val reader: Reader, private val id2typeSerializer: Map<Int, TypeSerializer>) {
    var objects: MutableList<Any>? = null
    fun read(): Any? {
        val id = reader.readVarInt()
        return (id2typeSerializer[id] ?: error("no type with id $id")).read(this)
    }
}

internal class Output(val writer: Writer, private val class2typeDesc: Map<Class<*>, TypeDesc>) {
    var object2reference: MutableMap<Any, Int>? = null
    fun write(value: Any?): Unit = when (value) {
        null -> NullTypeDesc.write(this, null)
        is List<*> -> ListTypeDesc.write(this, value)
        else -> (class2typeDesc[value.javaClass] ?: error("missing type '${value.javaClass.canonicalName}'"))
            .write(this, value)
    }
}

enum class WireType {
    Intern,
    Binary,
    VarInt,
    Bytes1,
    Bytes2,
    Bytes4,
    Bytes8
}

abstract class TypeSerializer internal constructor(val type: Class<*>, val wireType: WireType) {
    internal abstract fun read(input: Input): Any?
    internal abstract fun write(output: Output, value: Any?)
    internal open fun write(output: Output, id: Int, value: Any?) {
        output.writer.writeVarInt(id)
        write(output, value)
    }
}

class TypeDesc(val id: Int, val serializer: TypeSerializer) {
    init {
        require(id >= 0) { "id $id for type '${serializer.type.canonicalName}' must be >= 0" }
    }

    internal fun write(output: Output, value: Any?) = serializer.write(output, id, value)
}

private class VoidType

val NullTypeDesc = TypeDesc(0, object : TypeSerializer(VoidType::class.java, WireType.Intern) {
    override fun read(input: Input): Any? = null
    override fun write(output: Output, value: Any?) {}
})

private class ReferenceType

val ReferenceTypeDesc = TypeDesc(1, object : TypeSerializer(ReferenceType::class.java, WireType.Intern) {
    override fun read(input: Input) = input.objects!![input.reader.readVarInt()]
    override fun write(output: Output, value: Any?) = output.writer.writeVarInt(value as Int)
})

val ListTypeDesc = TypeDesc(2, object : TypeSerializer(List::class.java, WireType.Intern) {
    override fun read(input: Input): List<*> {
        var length = input.reader.readVarInt()
        val list = mutableListOf<Any?>()
        while (length-- > 0) list.add(input.read())
        return list
    }

    override fun write(output: Output, value: Any?) {
        val list = value as List<*>
        output.writer.writeVarInt(list.size)
        for (e in list) output.write(e)
    }
})

const val FirstTypeId = 3

abstract class BaseTypeSerializer<V : Any> protected constructor(
    type: Class<V>, wireType: WireType
) : TypeSerializer(type, wireType) {
    init {
        check(wireType != WireType.Intern)
    }

    final override fun read(input: Input) = read(input.reader)
    @Suppress("UNCHECKED_CAST")
    final override fun write(output: Output, value: Any?) = write(output.writer, value as V)

    abstract fun write(writer: Writer, value: V)
    abstract fun read(reader: Reader): V
}

private val PrimitiveWrapperType = mapOf(
    Boolean::class.javaPrimitiveType to Boolean::class.javaObjectType,
    Byte::class.javaPrimitiveType to Byte::class.javaObjectType,
    Short::class.javaPrimitiveType to Short::class.javaObjectType,
    Int::class.javaPrimitiveType to Int::class.javaObjectType,
    Long::class.javaPrimitiveType to Long::class.javaObjectType,
    Char::class.javaPrimitiveType to Char::class.javaObjectType,
    Float::class.javaPrimitiveType to Float::class.javaObjectType,
    Double::class.javaPrimitiveType to Double::class.javaObjectType
)

fun primitiveWrapperType(type: Class<*>): Class<*> = PrimitiveWrapperType.getOrDefault(type, type)

class FieldSerializer internal constructor(val field: Field) {
    private var _typeSerializer: TypeSerializer? = null

    val typeSerializer: TypeSerializer? get() = _typeSerializer

    internal fun fixup(class2typeDesc: Map<Class<*>, TypeDesc>) {
        val typeDesc = class2typeDesc[primitiveWrapperType(field.type)]
        _typeSerializer = typeDesc?.serializer
        if (_typeSerializer is ClassTypeSerializer) _typeSerializer = null
    }

    internal fun read(input: Input, value: Any) =
        field.set(value, if (_typeSerializer == null) input.read() else _typeSerializer!!.read(input))

    internal fun write(output: Output, id: Int, value: Any) {
        val f = field.get(value)
        if (f != null) {
            output.writer.writeVarInt(id)
            if (_typeSerializer == null) output.write(f) else _typeSerializer!!.write(output, f)
        }
    }
}

private const val EndFieldId = 0
const val FirstFieldId = EndFieldId + 1

class FieldDesc internal constructor(val id: Int, val serializer: FieldSerializer)

class ClassTypeSerializer internal constructor(
    type: Class<*>, val graph: Boolean, private val id2fieldSerializer: Map<Int, FieldSerializer>
) : TypeSerializer(type, WireType.Intern) {
    private val allocator = AllocatorFactory(type)
    val fieldDescs: List<FieldDesc>

    init {
        val fds = mutableListOf<FieldDesc>()
        for ((id, serializer) in id2fieldSerializer) {
            require(id >= FirstFieldId) { "id $id for field '${serializer.field}' must be >= $FirstFieldId" }
            fds.add(FieldDesc(id, serializer))
        }
        fieldDescs = Collections.unmodifiableList(fds.sortedBy { it.id })
    }

    internal fun fixupFields(class2typeDesc: Map<Class<*>, TypeDesc>) =
        id2fieldSerializer.values.forEach { it.fixup(class2typeDesc) }

    override fun read(input: Input): Any {
        val value = allocator()
        if (graph) {
            if (input.objects == null) input.objects = mutableListOf()
            input.objects!!.add(value)
        }
        while (true) {
            val id = input.reader.readVarInt()
            if (id == EndFieldId) return value
            val fieldSerializer = id2fieldSerializer[id]
            if (fieldSerializer == null)
                error("class '${type.canonicalName}' doesn't have a field with id $id")
            else
                fieldSerializer.read(input, value)
        }
    }

    override fun write(output: Output, id: Int, value: Any?) {
        if (graph) {
            if (output.object2reference == null) output.object2reference = IdentityHashMap(16)
            val object2reference = output.object2reference
            val reference = object2reference!![value]
            if (reference != null) {
                ReferenceTypeDesc.write(output, reference)
                return
            }
            object2reference[value!!] = object2reference.size
        }
        super.write(output, id, value)
    }

    override fun write(output: Output, value: Any?) {
        for (fieldDesc in fieldDescs) fieldDesc.serializer.write(output, fieldDesc.id, value!!)
        output.writer.writeVarInt(EndFieldId)
    }
}

private val RootClasses = setOf(
    Any::class.java,
    Exception::class.java,
    RuntimeException::class.java,
    Error::class.java,
    Throwable::class.java
)

fun isRootClass(type: Class<*>): Boolean = RootClasses.contains(type)

/**
 * This fast and compact serializer supports the following types (type id's must be >= [FirstTypeId]):
 *  - `null`
 *  - Subclasses of [BaseTypeSerializer]
 *  - [List] (deserialize creates an [ArrayList])
 *  - enumeration types (an enumeration constant is serialized with its ordinal number)
 *  - class hierarchies with all non-static and non-transient fields
 *    (field names and id's must be unique in the path to its super classes and id's must be >= [FirstFieldId])
 *  - exceptions (but without fields of [Throwable]; therefore, you should implement [Throwable.message])
 *  - graphs with cycles
 */
abstract class FastSerializer protected constructor(val skipping: Boolean) : Serializer {
    private val class2typeDesc = HashMap<Class<*>, TypeDesc>(64)
    private val _id2typeSerializer = HashMap<Int, TypeSerializer>(64)

    init {
        check(!skipping) { "skipping is not yet implemented" }
        addType(NullTypeDesc)
        addType(ReferenceTypeDesc)
        addType(ListTypeDesc)
    }

    val id2typeSerializer: SortedMap<Int, TypeSerializer> get() = TreeMap(_id2typeSerializer)

    private fun addType(typeDesc: TypeDesc) {
        require(class2typeDesc.put(typeDesc.serializer.type, typeDesc) == null) {
            "type '${typeDesc.serializer.type.canonicalName}' already added"
        }
        val oldTypeSerializer = _id2typeSerializer.put(typeDesc.id, typeDesc.serializer)
        check(oldTypeSerializer == null) {
            "type id ${typeDesc.id} used for '${typeDesc.serializer.type.canonicalName}' " +
                "and '${oldTypeSerializer!!.type.canonicalName}'"
        }
    }

    protected fun addEnum(id: Int, type: Class<*>) {
        require(type.isEnum) { "type '${type.canonicalName}' is not an enumeration" }
        @Suppress("UNCHECKED_CAST") val enumeration = type as Class<Enum<*>>
        val constants = enumeration.enumConstants
        addType(TypeDesc(id, object : BaseTypeSerializer<Enum<*>>(enumeration, WireType.VarInt) {
            override fun read(reader: Reader) = constants[reader.readVarInt()]
            override fun write(writer: Writer, value: Enum<*>) = writer.writeVarInt(value.ordinal)
        }))
    }

    protected fun checkClass(type: Class<*>) =
        require(!type.isEnum) { "type '${type.canonicalName}' is an enumeration" }

    protected fun addClass(id: Int, type: Class<*>, graph: Boolean, id2field: Map<Int, Field>) {
        require(!Modifier.isAbstract(type.modifiers)) { "type '${type.canonicalName}' is abstract" }
        val id2fieldSerializer = mutableMapOf<Int, FieldSerializer>()
        val name2field = mutableMapOf<String, Field>()
        for ((fieldId, field) in id2field) {
            val oldField = name2field.put(field.name, field)
            require(oldField == null) {
                "duplicated field name '$field' and '$oldField' not allowed in class hierarchy"
            }
            id2fieldSerializer[fieldId] = FieldSerializer(field)
        }
        addType(TypeDesc(id, ClassTypeSerializer(type, graph, id2fieldSerializer)))
    }

    protected fun addBaseType(typeDesc: TypeDesc) {
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
        for (typeDesc in class2typeDesc.values)
            (typeDesc.serializer as? ClassTypeSerializer)?.fixupFields(class2typeDesc)
        checkParentClasses()
    }

    override fun read(reader: Reader) = Input(reader, _id2typeSerializer).read()
    override fun write(writer: Writer, value: Any?) = Output(writer, class2typeDesc).write(value)
}
