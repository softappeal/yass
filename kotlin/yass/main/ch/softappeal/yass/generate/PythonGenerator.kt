package ch.softappeal.yass.generate

import ch.softappeal.yass.ownFields
import ch.softappeal.yass.remote.Services
import ch.softappeal.yass.remote.SimpleMethodMapperFactory
import ch.softappeal.yass.serialize.fast.BTH_BOOLEAN
import ch.softappeal.yass.serialize.fast.BTH_BYTE_ARRAY
import ch.softappeal.yass.serialize.fast.BTH_DOUBLE
import ch.softappeal.yass.serialize.fast.BTH_STRING
import ch.softappeal.yass.serialize.fast.BaseTypeHandler
import ch.softappeal.yass.serialize.fast.ClassTypeHandler
import ch.softappeal.yass.serialize.fast.FIRST_TYPE_ID
import ch.softappeal.yass.serialize.fast.FastSerializer
import ch.softappeal.yass.serialize.fast.FieldDesc
import ch.softappeal.yass.serialize.fast.TD_LIST
import ch.softappeal.yass.serialize.fast.TypeDesc
import ch.softappeal.yass.serialize.fast.TypeHandler
import ch.softappeal.yass.serialize.fast.primitiveWrapperType
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.ArrayList
import java.util.Arrays
import java.util.Comparator
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Objects
import java.util.TreeMap
import java.util.TreeSet
import java.util.function.Consumer

class PythonExternalDesc(internal val name: String, internal val typeDesc: String)

/** You must use the "-parameters" option for javac to get the real method parameter names. */
class PythonGenerator constructor(
    rootPackage: String, serializer: FastSerializer, initiator: Services, acceptor: Services, private val python3: Boolean,
    private val includeFileForEachModule: String?, module2includeFile: Map<String, String>?, externalTypes: Map<Class<*>, PythonExternalDesc>?, generatedDir: String
) : Generator(rootPackage, serializer, initiator, acceptor) {
    private val module2includeFile = HashMap<String, String>()
    private val externalTypes = TreeMap<Class<*>, PythonExternalDesc>(Comparator.comparing<Class<*>, String>({ it.getCanonicalName() }))
    private val rootNamespace = Namespace(null, null, ROOT_MODULE, 0)
    private val type2namespace = LinkedHashMap<Class<*>, Namespace>()
    private val type2id = HashMap<Class<*>, Int>()

    private inner class Namespace internal constructor(internal val parent: Namespace?, internal val name: String?, moduleName: String, internal val depth: Int) {
        internal val moduleName: String
        internal val types = LinkedHashSet<Class<*>>()
        private val children = HashMap<String, Namespace>()

        init {
            this.moduleName = Objects.requireNonNull(moduleName)
        }

        private fun add(qualifiedName: String, type: Class<*>) {
            val dot = qualifiedName.indexOf('.')
            if (dot < 0) { // leaf
                checkType(type)
                if (!type.isEnum && !type.isInterface) { // note: baseclasses must be before subclasses
                    val superClass = type.superclass
                    if (!isRootClass(superClass)) {
                        rootNamespace.add(superClass)
                    }
                }
                types.add(type)
                type2namespace[type] = this
            } else { // intermediate
                (children as MutableMap<String, Namespace>).computeIfAbsent(
                    qualifiedName.substring(0, dot)
                ) { name -> Namespace(this, name, moduleName + '_'.toString() + name, depth + 1) }.add(qualifiedName.substring(dot + 1), type)
            }
        }

        internal fun add(type: Class<*>) {
            add(qualifiedName(type), type)
        }

        internal fun generate(path: String) {
            try {
                ContractPythonOut(Objects.requireNonNull(path) + INIT_PY, this)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

            children.forEach { name, namespace -> namespace.generate(path + '/'.toString() + name) }
        }
    }

    init {
        module2includeFile?.forEach { m, i -> this.module2includeFile[Objects.requireNonNull(m)] = Objects.requireNonNull(i) }
        externalTypes?.forEach { java, py -> this.externalTypes[Objects.requireNonNull(java)] = Objects.requireNonNull(py) }
        id2typeHandler.forEach { id, typeHandler ->
            if (id >= FIRST_DESC_ID) {
                val type = typeHandler.type
                type2id[type] = id
                if (!this.externalTypes.containsKey(type)) {
                    rootNamespace.add(type)
                }
            }
        }
        interfaces.forEach(Consumer { rootNamespace.add(it) })
        rootNamespace.generate(generatedDir + '/'.toString() + ROOT_MODULE)
        MetaPythonOut(generatedDir + INIT_PY)
    }

    private fun typeHandler(type: Class<*>): TypeHandler {
        return Objects.requireNonNull<TypeHandler>(id2typeHandler[Objects.requireNonNull<Int>(type2id[Objects.requireNonNull(type)])])
    }

    private fun hasClassDesc(type: Class<*>): Boolean {
        return !type.isEnum && !Modifier.isAbstract(type.modifiers) && typeHandler(type) is ClassTypeHandler
    }

    private inner class ContractPythonOut internal constructor(file: String, namespace: Namespace) : Out(file) {
        private val namespace: Namespace
        private val modules = TreeSet(Comparator.comparing<Namespace, String> { n -> n.moduleName })
        private fun getQualifiedName(type: Class<*>): String {
            val ns = Objects.requireNonNull<Namespace>(type2namespace[Objects.requireNonNull(type)])
            modules.add(ns)
            return (if (namespace != ns) ns.moduleName + '.' else "") + type.simpleName
        }

        private fun importModule(module: Namespace) {
            print("from ")
            for (d in namespace.depth + 2 downTo 1) {
                print(".")
            }
            if (module == rootNamespace) {
                println(" import $ROOT_MODULE")
            } else {
                printfln("%s import %s as %s", module.parent!!.moduleName.replace('_', '.'), module.name!!, module.moduleName)
            }
        }

        init {
            this.namespace = Objects.requireNonNull(namespace)
            println("from enum import Enum")
            println("from typing import List, Any, cast")
            println()
            println("import yass")
            if (includeFileForEachModule != null) {
                includeFile(includeFileForEachModule)
            }
            val moduleIncludeFile = module2includeFile[if (namespace == rootNamespace) "" else namespace.moduleName.substring(ROOT_MODULE.length + 1).replace('_', '.')]
            if (moduleIncludeFile != null) {
                println2()
                includeFile(moduleIncludeFile)
            }
            val buffer = StringBuilder()
            redirect(buffer)
            @Suppress("UNCHECKED_CAST") namespace.types.stream().filter { it.isEnum() }.forEach { type -> generateEnum(type as Class<Enum<*>>) }
            namespace.types.stream()
                .filter { t -> !t.isEnum && !t.isInterface && (Modifier.isAbstract(t.modifiers) || typeHandler(t) is ClassTypeHandler) }
                .forEach(Consumer<Class<*>> { this.generateClass(it) })
            namespace.types.stream().filter({ it.isInterface() }).forEach(Consumer<Class<*>> { this.generateInterface(it) })
            redirect(null)
            modules.stream().filter { module -> module != namespace }.forEach(Consumer<Namespace> { this.importModule(it) })
            print(buffer)
            close()
        }

        private fun generateEnum(type: Class<out Enum<*>>) {
            println2()
            tabsfln("class %s(Enum):", type.simpleName)
            for (e in type.enumConstants) {
                tab()
                tabsfln("%s = %s", e.name, e.ordinal)
            }
        }

        private fun pythonType(type: Type): String {
            if (type is ParameterizedType) {
                return if (type.rawType === List::class.java) {
                    "List[" + pythonType(type.actualTypeArguments[0]) + ']'.toString()
                } else {
                    throw RuntimeException("unexpected type '" + type + '\''.toString())
                }
            } else if (type === Void.TYPE) {
                return "None"
            } else if (type === Double::class.javaPrimitiveType || type === Double::class.javaObjectType) {
                return "float"
            } else if (type === Boolean::class.javaPrimitiveType || type === Boolean::class.javaObjectType) {
                return "bool"
            } else if (type === String::class.java) {
                return if (python3) "str" else "unicode"
            } else if (type === ByteArray::class.java) {
                return "bytes"
            } else if (type === Any::class.java) {
                return "Any"
            } else if (Throwable::class.java.isAssignableFrom(type as Class<*>)) {
                return "Exception"
            }
            val externalDesc = externalTypes[primitiveWrapperType(type)]
            if (externalDesc != null) {
                return externalDesc.name
            }
            checkType(type)
            if (type.isArray) {
                throw IllegalArgumentException("illegal type " + type.canonicalName + " (use List instead [])")
            }
            return getQualifiedName(type)
        }

        private fun generateClass(type: Class<*>) {
            println2()
            val sc = type.superclass
            val superClass = if (isRootClass(sc)) null else sc
            if (Modifier.isAbstract(type.modifiers)) {
                println("@yass.abstract")
            }
            tabsf("class %s", type.simpleName)
            var hasSuper = false
            if (superClass != null) {
                hasSuper = true
                printf("(%s)", getQualifiedName(superClass))
            } else if (Throwable::class.java.isAssignableFrom(sc)) {
                print("(Exception)")
            }
            println(":")
            inc()
            tabsfln("def __init__(self)%s", if (python3) " -> None:" else ":  # type: () -> None")
            inc()
            if (hasSuper) {
                tabsfln("%s.__init__(self)", getQualifiedName(superClass!!))
            }
            val ownFields = ownFields(type)
            if (ownFields.isEmpty() && !hasSuper) {
                tabsln("pass")
            } else {
                for (field in ownFields) {
                    val t = pythonType(field.genericType)
                    if (python3) {
                        tabsfln("self.%s: %s = cast(%s, None)", field.name, t, t)
                    } else {
                        tabsfln("self.%s = cast('%s', None)  # type: %s", field.name, t, t)
                    }
                }
            }
            dec()
            dec()
        }

        private fun generateInterface(type: Class<*>) {
            SimpleMethodMapperFactory.invoke(type) // checks for overloaded methods (Python restriction)
            val methodMapper = methodMapper(type)
            println2()
            printfln("class %s:", type.simpleName)
            inc()
            iterate(Arrays.asList(*getMethods(type)), Runnable { this.println() }, Consumer { method ->
                tabsf("def %s(self", method.getName())
                val parameters = method.getParameters()
                if (python3) {
                    Arrays.stream<Parameter>(parameters).forEach { parameter -> printf(", %s: %s", parameter.name, pythonType(parameter.parameterizedType)) }
                    printfln(") -> %s:", if (methodMapper.map(method).oneWay) "None" else pythonType(method.getGenericReturnType()))
                } else {
                    Arrays.stream<Parameter>(parameters).forEach { parameter -> printf(", %s", parameter.name) }
                    print("):  # type: (")
                    iterate(Arrays.asList<Parameter>(*parameters), Runnable { print(", ") }, Consumer { parameter -> print(pythonType(parameter.getParameterizedType())) })
                    printfln(") -> %s", if (methodMapper.map(method).oneWay) "None" else pythonType(method.getGenericReturnType()))
                }
                tab()
                tabsln("raise NotImplementedError()")
            })
            dec()
        }
    }

    private inner class MetaPythonOut internal constructor(file: String) : Out(file) {
        private fun getQualifiedName(type: Class<*>): String {
            val ns = Objects.requireNonNull<Namespace>(type2namespace[Objects.requireNonNull(type)])
            return ns.moduleName + '.'.toString() + type.simpleName
        }

        private fun importModule(module: Namespace) {
            print("from .")
            if (module == rootNamespace) {
                println(" import $ROOT_MODULE")
            } else {
                printfln("%s import %s as %s", module.parent!!.moduleName.replace('_', '.'), module.name!!, module.moduleName)
            }
        }

        init {
            println("import yass")
            if (includeFileForEachModule != null) {
                includeFile(includeFileForEachModule)
            }
            LinkedHashSet(type2namespace.values).forEach(Consumer<Namespace> { this.importModule(it) })
            println()
            type2namespace.keys.forEach { type ->
                val qn = getQualifiedName(type)
                if (type.isEnum) {
                    printfln("yass.enumDesc(%s, %s)", type2id[type]!!, qn)
                } else if (hasClassDesc(type)) {
                    printfln("yass.classDesc(%s, %s, %s)", type2id[type]!!, qn, pyBool((typeHandler(type) as ClassTypeHandler).graph))
                }
            }
            println()
            type2namespace.keys.stream().filter({ this@PythonGenerator.hasClassDesc(it) }).forEach { type ->
                tabsfln("yass.fieldDescs(%s, [", getQualifiedName(type))
                for (fieldDesc in (typeHandler(type) as ClassTypeHandler).fieldDescs) {
                    tab()
                    tabsfln("yass.FieldDesc(%s, '%s', %s),", fieldDesc.id, fieldDesc.handler.field.name, typeDesc(fieldDesc))
                }
                tabsln("])")
            }
            println()
            println("SERIALIZER = yass.FastSerializer([")
            inc()
            externalTypes.values.forEach { externalDesc -> tabsfln("%s,", externalDesc.typeDesc) }
            type2namespace.keys.stream().filter { t -> !Modifier.isAbstract(t.modifiers) }.forEach { t -> tabsfln("%s,", getQualifiedName(t)) }
            dec()
            println("])")
            println()
            interfaces.forEach(Consumer<Class<*>> { this.generateMapper(it) })
            generateServices(initiator, "INITIATOR")
            generateServices(acceptor, "ACCEPTOR")
            close()
        }

        private fun generateServices(services: Services?, role: String) {
            if (services == null) {
                return
            }
            println2()
            tabsfln("class %s:", role)
            for (sd in getServiceDescs(services)) {
                val qn = getQualifiedName(sd.contractId.contract)
                tab()
                if (python3) {
                    tabsfln("%s: yass.ContractId[%s] = yass.ContractId(%s, %s)", sd.name, qn, qn, sd.contractId.id)
                } else {
                    tabsfln("%s = yass.ContractId(%s, %s)  # type: yass.ContractId[%s]", sd.name, qn, sd.contractId.id, qn)
                }
            }
        }

        private fun typeDesc(fieldDesc: FieldDesc): String {
            val typeHandler = fieldDesc.handler.typeHandler() ?: return "None"
            if (TD_LIST.handler === typeHandler) {
                return "yass.LIST_DESC"
            } else if (BOOLEAN_DESC.handler === typeHandler) {
                return "yass.BOOLEAN_DESC"
            } else if (DOUBLE_DESC.handler === typeHandler) {
                return "yass.DOUBLE_DESC"
            } else if (STRING_DESC.handler === typeHandler) {
                return "yass.STRING_DESC"
            } else if (BYTES_DESC.handler === typeHandler) {
                return "yass.BYTES_DESC"
            }
            val externalDesc = externalTypes[primitiveWrapperType(typeHandler.type)]
            return externalDesc?.typeDesc ?: getQualifiedName(typeHandler.type)
        }

        private fun generateMapper(type: Class<*>) {
            val methodMapper = methodMapper(type)
            tabsfln("yass.methodMapper(%s, [", getQualifiedName(type))
            for (method in getMethods(type)) {
                val (method1, id, oneWay) = methodMapper.map(method)
                tab()
                tabsfln("yass.MethodMapping(%s, '%s', %s),", id, method1.name, pyBool(oneWay))
            }
            tabsln("])")
        }
    }

    companion object { // $todo remove companion

        val BOOLEAN_DESC = TypeDesc(FIRST_TYPE_ID, BTH_BOOLEAN)
        val DOUBLE_DESC = TypeDesc(FIRST_TYPE_ID + 1, BTH_DOUBLE)
        val STRING_DESC = TypeDesc(FIRST_TYPE_ID + 2, BTH_STRING)
        val BYTES_DESC = TypeDesc(FIRST_TYPE_ID + 3, BTH_BYTE_ARRAY)
        val FIRST_DESC_ID = FIRST_TYPE_ID + 4

        fun baseTypeHandlers(vararg handlers: BaseTypeHandler<*>): List<BaseTypeHandler<*>> {
            val h = ArrayList<BaseTypeHandler<*>>()
            h.add(BOOLEAN_DESC.handler as BaseTypeHandler<*>)
            h.add(DOUBLE_DESC.handler as BaseTypeHandler<*>)
            h.add(STRING_DESC.handler as BaseTypeHandler<*>)
            h.add(BYTES_DESC.handler as BaseTypeHandler<*>)
            h.addAll(Arrays.asList(*handlers))
            return h
        }

        fun baseTypeDescs(vararg descs: TypeDesc): Collection<TypeDesc> {
            val d = ArrayList<TypeDesc>()
            d.add(BOOLEAN_DESC)
            d.add(DOUBLE_DESC)
            d.add(STRING_DESC)
            d.add(BYTES_DESC)
            d.addAll(Arrays.asList(*descs))
            return d
        }

        private val INIT_PY = "/__init__.py"
        private val ROOT_MODULE = "contract"

        private fun pyBool(value: Boolean): String {
            return if (value) "True" else "False"
        }
    }

}
