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
import ch.softappeal.yass.serialize.fast.primitiveWrapperType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashMap
import java.util.Objects
import java.util.function.Consumer

class ExternalDesc(internal val name: String, internal val typeDescHolder: String)

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

private fun nullable(type: String): String {
    return "yass.Nullable<" + type + '>'.toString()
}

/**
 * You must use the "-parameters" option for javac to get the real method parameter names.
 * [additionalTypeCode] see [pull request](https://github.com/softappeal/yass/pull/4)
 */
class TypeScriptGenerator @JvmOverloads constructor(
    rootPackage: String, serializer: FastSerializer, initiator: Services, acceptor: Services,
    includeFile: String, externalTypes: Map<Class<*>, ExternalDesc>, contractFile: String, additionalTypeCode: String? = null
) : Generator(rootPackage, serializer, initiator, acceptor) {

    private inner class TypeScriptOut constructor(
        includeFile: String, externalTypes: Map<Class<*>, ExternalDesc>?, contractFile: String, private val additionalTypeCode: String?
    ) : Out(contractFile) {

        private val type2id = LinkedHashMap<Class<*>, Int>()
        private val visitedClasses = HashSet<Class<*>>()
        private val externalTypes = HashMap<Class<*>, ExternalDesc>()

        private fun jsType(type: Class<*>, externalName: Boolean): String {
            val externalDesc = externalTypes[primitiveWrapperType(type)]
            if (externalDesc != null) {
                return if (externalName) externalDesc.name else externalDesc.typeDescHolder
            }
            checkType(type)
            if (type.isArray) {
                throw IllegalArgumentException("illegal type " + type.canonicalName + " (use List instead [])")
            }
            return qualifiedName(type)
        }

        private fun generateType(type: Class<*>, typeGenerator: Consumer<String>) {
            checkType(type)
            val jsType = qualifiedName(type)
            val dot = jsType.lastIndexOf('.')
            val name = type.simpleName
            if (dot < 0) {
                typeGenerator.accept(name)
            } else {
                tabsfln("export namespace %s {", jsType.substring(0, dot))
                inc()
                typeGenerator.accept(name)
                dec()
                tabsln("}")
            }
            println()
        }

        private fun generateEnum(type: Class<out Enum<*>>) {
            generateType(type, Consumer { name ->
                tabsfln("export class %s extends yass.Enum {", name)
                inc()
                additionalTypeCode()
                for (e in type.enumConstants) {
                    tabsfln("static readonly %s = new %s(%s, '%s');", e.name, name, e.ordinal, e.name)
                }
                tabsfln("static readonly VALUES = <%s[]>yass.enumValues(%s);", name, name)
                tabsfln("static readonly TYPE_DESC = yass.enumDesc(%s, %s);", type2id[type]!!, name)
                dec()
                tabsln("}")
            })
        }

        private fun type(type: Type?): String {
            if (type is ParameterizedType) {
                if (type.rawType === List::class.java) {
                    return type(type.actualTypeArguments[0]) + "[]"
                } else {
                    val s = StringBuilder(type(type.rawType))
                    val actualTypeArguments = type.actualTypeArguments
                    if (actualTypeArguments.size != 0) {
                        s.append('<')
                        iterate(
                            listOf(*actualTypeArguments),
                            Runnable { s.append(", ") },
                            Consumer { actualTypeArgument -> s.append(type(actualTypeArgument)) }
                        )
                        s.append('>')
                    }
                    return s.toString()
                }
            } else if (type is TypeVariable<*>) {
                return type.name
            } else if (type === Double::class.javaPrimitiveType || type === Double::class.javaObjectType) {
                return "number"
            } else if (type === Boolean::class.javaPrimitiveType || type === Boolean::class.javaObjectType) {
                return "boolean"
            } else if (type === String::class.java) {
                return "string"
            } else if (type === ByteArray::class.java) {
                return "Uint8Array"
            } else if (isRootClass((type as Class<*>?)!!)) {
                return "any"
            } else if (type === Void.TYPE) {
                return "void"
            }
            return jsType(type as Class<*>, true)
        }

        private fun typeDesc(fieldDesc: FieldDesc): String {
            val typeHandler = fieldDesc.handler.typeHandler() ?: return "null"
            if (TD_LIST.handler === typeHandler) {
                return "yass.LIST_DESC"
            } else if (BOOLEAN_DESC.handler === typeHandler) {
                return "yass.BOOLEAN_DESC"
            } else if (DOUBLE_DESC.handler === typeHandler) {
                return "yass.NUMBER_DESC"
            } else if (STRING_DESC.handler === typeHandler) {
                return "yass.STRING_DESC"
            } else if (BYTES_DESC.handler === typeHandler) {
                return "yass.BYTES_DESC"
            }
            return jsType(typeHandler.type, false) + ".TYPE_DESC"
        }

        private fun <C> generateClass(type: Class<C>) {
            if (!visitedClasses.add(type)) {
                return
            }
            var sc: Type? = type.genericSuperclass
            if (sc is Class<*>) {
                if (isRootClass((sc as Class<*>?)!!)) {
                    sc = null
                } else {
                    generateClass(sc)
                }
            } else {
                generateClass((sc as ParameterizedType).rawType as Class<*>)
            }
            val superClass = sc
            generateType(type, Consumer { name ->
                tabsf("export %sclass %s", if (Modifier.isAbstract(type.modifiers)) "abstract " else "", name)
                val typeParameters = type.typeParameters
                if (typeParameters.size != 0) {
                    print("<")
                    iterate(listOf(*typeParameters), Runnable { print(", ") }, Consumer { typeParameter -> print(typeParameter.getName()) })
                    print(">")
                }
                if (superClass != null) {
                    print(" extends " + type(superClass))
                }
                println(" {")
                inc()
                additionalTypeCode()
                for (field in ownFields(type)) {
                    tabsfln("%s: %s;", field.name, nullable(type(field.genericType)))
                }
                val id = type2id[type]
                if (id != null) {
                    tabsf("static readonly TYPE_DESC = yass.classDesc(%s, %s", id, name)
                    inc()
                    val typeHandler = id2typeHandler[id] as ClassTypeHandler
                    if (typeHandler.graph) {
                        throw IllegalArgumentException("class '$type' is referenceable (not implemented in TypeScript)")
                    }
                    for (fieldDesc in typeHandler.fieldDescs) {
                        println(",")
                        tabsf("new yass.FieldDesc(%s, '%s', %s)", fieldDesc.id, fieldDesc.handler.field.name, typeDesc(fieldDesc))
                    }
                    println()
                    dec()
                    tabsln(");")
                }
                dec()
                tabsln("}")
            })
        }

        private fun generateInterface(type: Class<*>) {
            SimpleMethodMapperFactory.invoke(type) // checks for overloaded methods (JavaScript restriction)
            val methods = getMethods(type)
            val methodMapper = methodMapper(type)
            generateType(type, object : Consumer<String> {
                private fun generateInterface(name: String, implementation: Boolean) {
                    tabsfln("export namespace %s {", if (implementation) "impl" else "proxy")
                    inc()
                    tabsfln("export interface %s {", name)
                    inc()
                    for (method in methods) {
                        tabsf("%s(", method.name)
                        iterate(listOf(*method.parameters), Runnable { print(", ") }, Consumer { p -> printf("%s: %s", p.getName(), nullable(type(p.getParameterizedType()))) })
                        print("): ")
                        if (methodMapper.map(method).oneWay) {
                            print("void")
                        } else {
                            val t = nullable(type(method.genericReturnType))
                            if (implementation) print(t) else printf("Promise<%s>", t)
                        }
                        println(";")
                    }
                    dec()
                    tabsln("}")
                    dec()
                    tabsln("}")
                }

                override fun accept(name: String) {
                    generateInterface(name, false)
                    generateInterface(name, true)
                    tabsln("export namespace mapper {")
                    inc()
                    tabsf("export const %s = new yass.MethodMapper(", name)
                    inc()
                    iterate(Arrays.asList(*methods), Runnable { print(",") }, Consumer { method ->
                        println()
                        val mapping = methodMapper.map(method)
                        tabsf("new yass.MethodMapping('%s', %s, %s)", mapping.method.getName(), mapping.id, mapping.oneWay)
                    })
                    println()
                    dec()
                    tabsln(");")
                    dec()
                    tabsln("}")
                }
            })
        }

        private fun generateServices(services: Services?, role: String) {
            if (services == null) {
                return
            }
            tabsfln("export namespace %s {", role)
            inc()
            for (serviceDesc in getServiceDescs(services)) {
                var name = qualifiedName(serviceDesc.contractId.contract)
                var namespace = ""
                val dot = name.lastIndexOf('.') + 1
                if (dot > 0) {
                    namespace = name.substring(0, dot)
                    name = name.substring(dot)
                }
                tabsfln(
                    "export const %s = new yass.ContractId<%sproxy.%s, %simpl.%s>(%s, %smapper.%s);",
                    serviceDesc.name, namespace, name, namespace, name, serviceDesc.contractId.id, namespace, name
                )
            }
            dec()
            tabsln("}")
            println()
        }

        private fun additionalTypeCode() {
            if (additionalTypeCode != null) tabsln(additionalTypeCode)
        }

        init {
            externalTypes?.forEach { java, ts -> this.externalTypes[Objects.requireNonNull(java)] = Objects.requireNonNull(ts) }
            id2typeHandler.forEach { id, typeHandler ->
                if (id >= FIRST_DESC_ID) {
                    type2id[typeHandler.type] = id
                }
            }
            includeFile(includeFile)
            @Suppress("UNCHECKED_CAST") id2typeHandler.values.stream().map { typeHandler -> typeHandler.type }.filter({ it.isEnum() }).forEach { type -> generateEnum(type as Class<Enum<*>>) }
            id2typeHandler.values.stream().filter { typeHandler -> typeHandler is ClassTypeHandler }.forEach { typeHandler -> generateClass(typeHandler.type) }
            interfaces.forEach(Consumer { this.generateInterface(it) })
            generateServices(initiator, "initiator")
            generateServices(acceptor, "acceptor")
            tabs("export const SERIALIZER = new yass.FastSerializer(")
            inc()
            iterate(type2id.keys, Runnable { print(",") }, Consumer { type ->
                println()
                tabs(jsType(type, false))
            })
            println()
            dec()
            tabsln(");")
            close()
        }

    }

    init {
        TypeScriptOut(includeFile, externalTypes, contractFile, additionalTypeCode)
    }
}
