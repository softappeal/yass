package ch.softappeal.yass.generate.py

import ch.softappeal.yass.*
import ch.softappeal.yass.generate.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.fast.*
import java.lang.reflect.*
import java.nio.file.*
import java.util.*

class ExternalDesc(internal val name: String, internal val typeDesc: String)

val BooleanDesc = TypeDesc(FirstTypeId, BooleanSerializer)
val DoubleDesc = TypeDesc(FirstTypeId + 1, DoubleSerializer)
val StringDesc = TypeDesc(FirstTypeId + 2, StringSerializer)
val BytesDesc = TypeDesc(FirstTypeId + 3, ByteArraySerializer)
const val FirstDescId = FirstTypeId + 4

@SafeVarargs
fun baseTypeSerializers(vararg serializers: BaseTypeSerializer<*>): List<BaseTypeSerializer<*>> {
    val s = mutableListOf(
        BooleanDesc.serializer as BaseTypeSerializer<*>,
        DoubleDesc.serializer as BaseTypeSerializer<*>,
        StringDesc.serializer as BaseTypeSerializer<*>,
        BytesDesc.serializer as BaseTypeSerializer<*>
    )
    s.addAll(serializers)
    return s
}

@SafeVarargs
fun baseTypeDescs(vararg descs: TypeDesc): Collection<TypeDesc> {
    val d = mutableListOf(BooleanDesc, DoubleDesc, StringDesc, BytesDesc)
    d.addAll(descs)
    return d
}

private const val InitPy = "__init__.py"
private const val RootModule = "contract"

private fun pyBool(value: Boolean) = if (value) "True" else "False"

/** You must use the "-parameters" option for javac to get the real method parameter names. */
class PythonGenerator(
    rootPackage: String,
    serializer: FastSerializer,
    initiator: Services?,
    acceptor: Services?,
    private val python3: Boolean,
    private val includeFileForEachModule: Path?,
    module2includeFile: Map<String, Path>?,
    externalTypes: Map<Class<*>, ExternalDesc>,
    generatedDir: Path
) : Generator(rootPackage, serializer, initiator, acceptor) {
    private val module2includeFile = mutableMapOf<String, Path>()
    private val externalTypes =
        TreeMap<Class<*>, ExternalDesc>(Comparator.comparing<Class<*>, String> { it.canonicalName })
    private val rootNamespace = Namespace(null, null, RootModule, 0)
    private val type2namespace = LinkedHashMap<Class<*>, Namespace>()
    private val type2id = mutableMapOf<Class<*>, Int>()

    init {
        if (module2includeFile != null) this.module2includeFile.putAll(module2includeFile)
        this.externalTypes.putAll(externalTypes)
        id2typeSerializer.forEach { id, typeSerializer ->
            if (id >= FirstDescId) {
                val type = typeSerializer.type
                type2id[type] = id
                if (!this.externalTypes.containsKey(type)) rootNamespace.add(type)
            }
        }
        interfaces.forEach { rootNamespace.add(it) }
        rootNamespace.generate(generatedDir.resolve(RootModule))
        MetaPythonOut(generatedDir.resolve(InitPy))
    }

    private inner class Namespace(val parent: Namespace?, val name: String?, val moduleName: String, val depth: Int) {
        val types = LinkedHashSet<Class<*>>()
        val children = mutableMapOf<String, Namespace>()

        fun add(qualifiedName: String, type: Class<*>) {
            val dot = qualifiedName.indexOf('.')
            if (dot < 0) { // leaf
                checkType(type)
                if (!type.isEnum && !type.isInterface) { // note: base classes must be before subclasses
                    val superClass = type.superclass
                    if (!isRootClass(superClass)) rootNamespace.add(superClass)
                }
                types.add(type)
                type2namespace[type] = this
            } else // intermediate
                children.computeIfAbsent(qualifiedName.substring(0, dot)) {
                    Namespace(this, it, "${moduleName}_$it", depth + 1)
                }
                    .add(qualifiedName.substring(dot + 1), type)
        }

        fun add(type: Class<*>) {
            add(qualifiedName(type), type)
        }

        fun generate(path: Path) {
            ContractPythonOut(path.resolve(InitPy), this)
            children.forEach { name, namespace -> namespace.generate(path.resolve(name)) }
        }
    }

    private fun typeSerializer(type: Class<*>): TypeSerializer = id2typeSerializer[type2id[type]]!!

    private fun hasClassDesc(type: Class<*>): Boolean =
        !type.isEnum && !Modifier.isAbstract(type.modifiers) && typeSerializer(type) is ClassTypeSerializer

    private inner class ContractPythonOut(path: Path, val namespace: Namespace) : Out(path) {
        val modules = TreeSet(Comparator.comparing<Namespace, String> { it.moduleName })

        init {
            println("from enum import Enum")
            println("from typing import List, Any, cast")
            println()
            println("import yass")
            if (includeFileForEachModule != null) includeFile(includeFileForEachModule)
            val moduleIncludeFile = module2includeFile[
                if (namespace == rootNamespace)
                    ""
                else
                    namespace.moduleName.substring(RootModule.length + 1).replace('_', '.')
            ]
            if (moduleIncludeFile != null) {
                println2()
                includeFile(moduleIncludeFile)
            }
            val buffer = StringBuilder()
            redirect(buffer)
            @Suppress("UNCHECKED_CAST") namespace.types
                .filter { it.isEnum }
                .forEach { generateEnum(it as Class<Enum<*>>) }
            namespace.types
                .filter { t ->
                    !t.isEnum &&
                        !t.isInterface &&
                        (Modifier.isAbstract(t.modifiers) || typeSerializer(t) is ClassTypeSerializer)
                }
                .forEach { generateClass(it) }
            namespace.types.filter { it.isInterface }.forEach { generateInterface(it) }
            redirect(null)
            modules.filter { it != namespace }.forEach { importModule(it) }
            print(buffer)
            close()
        }

        fun getQualifiedName(type: Class<*>): String {
            val ns = type2namespace[type]!!
            modules.add(ns)
            return "${if (namespace != ns) ns.moduleName + '.' else ""}${type.simpleName}"
        }

        fun importModule(module: Namespace) {
            print("from ")
            for (d in namespace.depth + 2 downTo 1) print(".")
            if (module == rootNamespace)
                println(" import $RootModule")
            else
                println(
                    "${module.parent!!.moduleName.replace('_', '.')} " +
                        "import ${module.name} as ${module.moduleName}"
                )
        }

        fun generateEnum(type: Class<out Enum<*>>) {
            println2()
            tabsln("class ${type.simpleName}(Enum):")
            for (e in type.enumConstants) {
                tab()
                tabsln("${e.name} = ${e.ordinal}")
            }
        }

        fun pythonType(type: Type): String = when {
            type is ParameterizedType -> if (type.rawType === List::class.java)
                "List[${pythonType(type.actualTypeArguments[0])}]"
            else
                error("unexpected type '$type'")
            type === Void.TYPE -> "None"
            type === Double::class.javaPrimitiveType || type === Double::class.javaObjectType -> "float"
            type === Boolean::class.javaPrimitiveType || type === Boolean::class.javaObjectType -> "bool"
            type === String::class.java -> if (python3) "str" else "unicode"
            type === ByteArray::class.java -> "bytes"
            type === Any::class.java -> "Any"
            Throwable::class.java.isAssignableFrom(type as Class<*>) -> "Exception"
            else -> {
                val externalDesc = externalTypes[primitiveWrapperType(type)]
                if (externalDesc != null)
                    externalDesc.name
                else {
                    checkType(type)
                    check(!type.isArray) { "illegal type ${type.canonicalName} (use List instead [])" }
                    getQualifiedName(type)
                }
            }
        }

        fun generateClass(type: Class<*>) {
            println2()
            val sc = type.superclass
            val superClass = if (isRootClass(sc)) null else sc
            if (Modifier.isAbstract(type.modifiers)) println("@yass.abstract")
            tabs("class ${type.simpleName}")
            var hasSuper = false
            if (superClass != null) {
                hasSuper = true
                print("(${getQualifiedName(superClass)})")
            } else if (Throwable::class.java.isAssignableFrom(sc))
                print("(Exception)")
            println(":")
            inc()
            tabsln("def __init__(self)${if (python3) " -> None:" else ":  # type: () -> None"}")
            inc()
            if (hasSuper) tabsln("${getQualifiedName(superClass!!)}.__init__(self)")
            val ownFields = ownFields(type)
            if (ownFields.isEmpty() && !hasSuper)
                tabsln("pass")
            else {
                for (field in ownFields) {
                    val t = pythonType(field.genericType)
                    if (python3)
                        tabsln("self.${field.name}: $t = cast($t, None)")
                    else
                        tabsln("self.${field.name} = cast('$t', None)  # type: $t")
                }
            }
            dec()
            dec()
        }

        fun generateInterface(type: Class<*>) {
            SimpleMethodMapperFactory.invoke(type) // checks for overloaded methods (Python restriction)
            println2()
            println("class ${type.simpleName}:")
            inc()
            val methodMapper = methodMapper(type)
            iterate(getMethods(type), { println() }, { method ->
                tabs("def ${method.name}(self")
                val parameters = method.parameters
                if (python3) {
                    parameters.forEach { parameter ->
                        print(", ${parameter.name}: ${pythonType(parameter.parameterizedType)}")
                    }
                    println(
                        ") -> ${if (methodMapper.map(method).oneWay) "None" else pythonType(method.genericReturnType)}:"
                    )
                } else {
                    parameters.forEach { parameter -> print(", ${parameter.name}") }
                    print("):  # type: (")
                    iterate(parameters.asList(), { print(", ") }, { print(pythonType(it.parameterizedType)) })
                    println(
                        ") -> ${if (methodMapper.map(method).oneWay) "None" else pythonType(method.genericReturnType)}"
                    )
                }
                tab()
                tabsln("raise NotImplementedError()")
            })
            dec()
        }
    }

    private inner class MetaPythonOut(path: Path) : Out(path) {
        init {
            println("import yass")
            if (includeFileForEachModule != null) includeFile(includeFileForEachModule)
            LinkedHashSet(type2namespace.values).forEach { importModule(it) }
            println()
            type2namespace.keys.forEach { type ->
                val qn = getQualifiedName(type)
                if (type.isEnum)
                    println("yass.enumDesc(${type2id[type]}, $qn)")
                else if (hasClassDesc(type))
                    println(
                        "yass.classDesc(${type2id[type]}, $qn, " +
                            "${pyBool((typeSerializer(type) as ClassTypeSerializer).graph)})"
                    )
            }
            println()
            type2namespace.keys.filter { hasClassDesc(it) }.forEach { type ->
                tabsln("yass.fieldDescs(${getQualifiedName(type)}, [")
                for (fieldDesc in (typeSerializer(type) as ClassTypeSerializer).fieldDescs) {
                    tab()
                    tabsln(
                        "yass.FieldDesc(${fieldDesc.id}, '${fieldDesc.serializer.field.name}', " +
                            "${typeDesc(fieldDesc)}),"
                    )
                }
                tabsln("])")
            }
            println()
            println("SERIALIZER = yass.FastSerializer([")
            inc()
            externalTypes.values.forEach { externalDesc -> tabsln("${externalDesc.typeDesc},") }
            type2namespace.keys
                .filter { t -> !Modifier.isAbstract(t.modifiers) }
                .forEach { t -> tabsln("${getQualifiedName(t)},") }
            dec()
            println("])")
            println()
            interfaces.forEach { generateMapper(it) }
            generateServices(initiator, "INITIATOR")
            generateServices(acceptor, "ACCEPTOR")
            close()
        }

        fun getQualifiedName(type: Class<*>): String {
            val ns = type2namespace[type]!!
            return "${ns.moduleName}.${type.simpleName}"
        }

        fun importModule(module: Namespace) {
            print("from .")
            if (module == rootNamespace)
                println(" import $RootModule")
            else
                println(
                    "${module.parent!!.moduleName.replace('_', '.')} " +
                        "import ${module.name} as ${module.moduleName}"
                )
        }

        fun generateServices(services: Services?, role: String) {
            if (services == null) return
            println2()
            tabsln("class $role:")
            for (sd in getServiceDescs(services)) {
                val qn = getQualifiedName(sd.contractId.contract)
                tab()
                if (python3)
                    tabsln("${sd.name}: yass.ContractId[$qn] = yass.ContractId($qn, ${sd.contractId.id})")
                else
                    tabsln("${sd.name} = yass.ContractId($qn, ${sd.contractId.id})  # type: yass.ContractId[$qn]")
            }
        }

        fun typeDesc(fieldDesc: FieldDesc): String {
            val typeSerializer = fieldDesc.serializer.typeSerializer ?: return "None"
            return when {
                ListTypeDesc.serializer === typeSerializer -> "yass.LIST_DESC"
                BooleanDesc.serializer === typeSerializer -> "yass.BOOLEAN_DESC"
                DoubleDesc.serializer === typeSerializer -> "yass.DOUBLE_DESC"
                StringDesc.serializer === typeSerializer -> "yass.STRING_DESC"
                BytesDesc.serializer === typeSerializer -> "yass.BYTES_DESC"
                else -> {
                    val externalDesc = externalTypes[primitiveWrapperType(typeSerializer.type)]
                    externalDesc?.typeDesc ?: getQualifiedName(typeSerializer.type)
                }
            }
        }

        fun generateMapper(type: Class<*>) {
            val methodMapper = methodMapper(type)
            tabsln("yass.methodMapper(${getQualifiedName(type)}, [")
            for (m in getMethods(type)) {
                val (method, id, oneWay) = methodMapper.map(m)
                tab()
                tabsln("yass.MethodMapping($id, '${method.name}', ${pyBool(oneWay)}),")
            }
            tabsln("])")
        }
    }
}
