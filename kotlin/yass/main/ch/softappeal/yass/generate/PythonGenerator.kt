@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.generate.py

import ch.softappeal.yass.generate.Generator
import ch.softappeal.yass.generate.Out
import ch.softappeal.yass.generate.getMethods
import ch.softappeal.yass.generate.getServiceDescs
import ch.softappeal.yass.generate.isRootClass
import ch.softappeal.yass.generate.iterate
import ch.softappeal.yass.ownFields
import ch.softappeal.yass.remote.Services
import ch.softappeal.yass.remote.SimpleMethodMapperFactory
import ch.softappeal.yass.serialize.fast.BaseTypeSerializer
import ch.softappeal.yass.serialize.fast.BooleanSerializer
import ch.softappeal.yass.serialize.fast.ByteArraySerializer
import ch.softappeal.yass.serialize.fast.ClassTypeSerializer
import ch.softappeal.yass.serialize.fast.DoubleSerializer
import ch.softappeal.yass.serialize.fast.FastSerializer
import ch.softappeal.yass.serialize.fast.FieldDesc
import ch.softappeal.yass.serialize.fast.FirstTypeId
import ch.softappeal.yass.serialize.fast.ListTypeDesc
import ch.softappeal.yass.serialize.fast.StringSerializer
import ch.softappeal.yass.serialize.fast.TypeDesc
import ch.softappeal.yass.serialize.fast.TypeSerializer
import ch.softappeal.yass.serialize.fast.primitiveWrapperType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.Comparator
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.TreeMap
import java.util.TreeSet

class ExternalDesc(internal val name: String, internal val typeDesc: String)

val BooleanDesc = TypeDesc(FirstTypeId, BooleanSerializer)
val DoubleDesc = TypeDesc(FirstTypeId + 1, DoubleSerializer)
val StringDesc = TypeDesc(FirstTypeId + 2, StringSerializer)
val BytesDesc = TypeDesc(FirstTypeId + 3, ByteArraySerializer)
const val FirstDescId = FirstTypeId + 4

fun baseTypeSerializers(vararg handlers: BaseTypeSerializer<*>): List<BaseTypeSerializer<*>> {
    val h = mutableListOf(
        BooleanDesc.handler as BaseTypeSerializer<*>,
        DoubleDesc.handler as BaseTypeSerializer<*>,
        StringDesc.handler as BaseTypeSerializer<*>,
        BytesDesc.handler as BaseTypeSerializer<*>
    )
    h.addAll(handlers)
    return h
}

fun baseTypeDescs(vararg descs: TypeDesc): Collection<TypeDesc> {
    val d = mutableListOf(BooleanDesc, DoubleDesc, StringDesc, BytesDesc)
    d.addAll(descs)
    return d
}

private const val INIT_PY = "/__init__.py"
private const val ROOT_MODULE = "contract"

private fun pyBool(value: Boolean) = if (value) "True" else "False"

/** You must use the "-parameters" option for javac to get the real method parameter names. */
class PythonGenerator(
    rootPackage: String, serializer: FastSerializer, initiator: Services?, acceptor: Services?, private val python3: Boolean,
    private val includeFileForEachModule: String?, module2includeFile: Map<String, String>?,
    externalTypes: Map<Class<*>, ExternalDesc>, generatedDir: String
) : Generator(rootPackage, serializer, initiator, acceptor) {
    private val module2includeFile = mutableMapOf<String, String>()
    private val externalTypes = TreeMap<Class<*>, ExternalDesc>(Comparator.comparing<Class<*>, String> { it.canonicalName })
    private val rootNamespace = Namespace(null, null, ROOT_MODULE, 0)
    private val type2namespace = LinkedHashMap<Class<*>, Namespace>()
    private val type2id = mutableMapOf<Class<*>, Int>()

    init {
        if (module2includeFile != null) this.module2includeFile.putAll(module2includeFile)
        this.externalTypes.putAll(externalTypes)
        id2typeHandler.forEach { id, typeHandler ->
            if (id >= FirstDescId) {
                val type = typeHandler.type
                type2id[type] = id
                if (!this.externalTypes.containsKey(type)) rootNamespace.add(type)
            }
        }
        interfaces.forEach { rootNamespace.add(it) }
        rootNamespace.generate("$generatedDir/$ROOT_MODULE")
        MetaPythonOut("$generatedDir$INIT_PY")
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
                children.computeIfAbsent(qualifiedName.substring(0, dot)) { Namespace(this, it, "${moduleName}_$it", depth + 1) }
                    .add(qualifiedName.substring(dot + 1), type)
        }

        fun add(type: Class<*>) {
            add(qualifiedName(type), type)
        }

        fun generate(path: String) {
            ContractPythonOut("$path$INIT_PY", this)
            children.forEach { name, namespace -> namespace.generate("$path/$name") }
        }
    }

    fun typeHandler(type: Class<*>): TypeSerializer = id2typeHandler[type2id[type]]!!

    fun hasClassDesc(type: Class<*>): Boolean =
        !type.isEnum && !Modifier.isAbstract(type.modifiers) && typeHandler(type) is ClassTypeSerializer

    private inner class ContractPythonOut(file: String, val namespace: Namespace) : Out(file) {
        val modules = TreeSet(Comparator.comparing<Namespace, String> { it.moduleName })

        init {
            println("from enum import Enum")
            println("from typing import List, Any, cast")
            println()
            println("import yass")
            if (includeFileForEachModule != null) includeFile(includeFileForEachModule)
            val moduleIncludeFile = module2includeFile[
                if (namespace == rootNamespace) "" else namespace.moduleName.substring(ROOT_MODULE.length + 1).replace('_', '.')
            ]
            if (moduleIncludeFile != null) {
                println2()
                includeFile(moduleIncludeFile)
            }
            val buffer = StringBuilder()
            redirect(buffer)
            @Suppress("UNCHECKED_CAST") namespace.types.filter { it.isEnum }.forEach { generateEnum(it as Class<Enum<*>>) }
            namespace.types
                .filter { t -> !t.isEnum && !t.isInterface && (Modifier.isAbstract(t.modifiers) || typeHandler(t) is ClassTypeSerializer) }
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
                println(" import $ROOT_MODULE")
            else
                println("${module.parent!!.moduleName.replace('_', '.')} import ${module.name} as ${module.moduleName}")
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
                    parameters.forEach { parameter -> print(", ${parameter.name}: ${pythonType(parameter.parameterizedType)}") }
                    println(") -> ${if (methodMapper.map(method).oneWay) "None" else pythonType(method.genericReturnType)}:")
                } else {
                    parameters.forEach { parameter -> print(", ${parameter.name}") }
                    print("):  # type: (")
                    iterate(parameters.asList(), { print(", ") }, { print(pythonType(it.parameterizedType)) })
                    println(") -> ${if (methodMapper.map(method).oneWay) "None" else pythonType(method.genericReturnType)}")
                }
                tab()
                tabsln("raise NotImplementedError()")
            })
            dec()
        }
    }

    private inner class MetaPythonOut(file: String) : Out(file) {
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
                    println("yass.classDesc(${type2id[type]}, $qn, ${pyBool((typeHandler(type) as ClassTypeSerializer).graph)})")
            }
            println()
            type2namespace.keys.filter { hasClassDesc(it) }.forEach { type ->
                tabsln("yass.fieldDescs(${getQualifiedName(type)}, [")
                for (fieldDesc in (typeHandler(type) as ClassTypeSerializer).fieldDescs) {
                    tab()
                    tabsln("yass.FieldDesc(${fieldDesc.id}, '${fieldDesc.handler.field.name}', ${typeDesc(fieldDesc)}),")
                }
                tabsln("])")
            }
            println()
            println("SERIALIZER = yass.FastSerializer([")
            inc()
            externalTypes.values.forEach { externalDesc -> tabsln("${externalDesc.typeDesc},") }
            type2namespace.keys.filter { t -> !Modifier.isAbstract(t.modifiers) }.forEach { t -> tabsln("${getQualifiedName(t)},") }
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
                println(" import $ROOT_MODULE")
            else
                println("${module.parent!!.moduleName.replace('_', '.')} import ${module.name} as ${module.moduleName}")
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
            val typeHandler = fieldDesc.handler.typeHandler() ?: return "None"
            return when {
                ListTypeDesc.handler === typeHandler -> "yass.LIST_DESC"
                BooleanDesc.handler === typeHandler -> "yass.BOOLEAN_DESC"
                DoubleDesc.handler === typeHandler -> "yass.DOUBLE_DESC"
                StringDesc.handler === typeHandler -> "yass.STRING_DESC"
                BytesDesc.handler === typeHandler -> "yass.BYTES_DESC"
                else -> {
                    val externalDesc = externalTypes[primitiveWrapperType(typeHandler.type)]
                    externalDesc?.typeDesc ?: getQualifiedName(typeHandler.type)
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
