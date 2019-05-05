package ch.softappeal.yass.generate.ts

import ch.softappeal.yass.*
import ch.softappeal.yass.generate.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.fast.*
import java.lang.reflect.*
import java.nio.file.*
import java.util.*

class ExternalDesc(internal val name: String, internal val typeDescHolder: String)

val BooleanDesc = TypeDesc(FirstTypeId, BooleanSerializer)
val DoubleDesc = TypeDesc(FirstTypeId + 1, DoubleSerializerNoSkipping)
val StringDesc = TypeDesc(FirstTypeId + 2, StringSerializer)
val BinaryDesc = TypeDesc(FirstTypeId + 3, BinarySerializer)
const val FirstDescId = FirstTypeId + 4

@SafeVarargs
fun baseTypeSerializers(vararg serializers: BaseTypeSerializer<*>): List<BaseTypeSerializer<*>> {
    val s = mutableListOf(
        BooleanDesc.serializer as BaseTypeSerializer<*>,
        DoubleDesc.serializer as BaseTypeSerializer<*>,
        StringDesc.serializer as BaseTypeSerializer<*>,
        BinaryDesc.serializer as BaseTypeSerializer<*>
    )
    s.addAll(serializers)
    return s
}

@SafeVarargs
fun baseTypeDescs(vararg descs: TypeDesc): Collection<TypeDesc> {
    val d = mutableListOf(BooleanDesc, DoubleDesc, StringDesc, BinaryDesc)
    d.addAll(descs)
    return d
}

private fun nullable(type: String): String = "yass.Nullable<$type>"

/**
 * You must use the "-parameters" option for javac to get the real method parameter names.
 * additionalTypeCode: see [pull request](https://github.com/softappeal/yass/pull/4)
 */
class TypeScriptGenerator @JvmOverloads constructor(
    rootPackage: String,
    serializer: FastSerializer,
    initiator: Services?,
    acceptor: Services?,
    includeFile: Path,
    externalTypes: Map<Class<*>, ExternalDesc>,
    contractFile: Path,
    additionalTypeCode: String? = null
) : Generator(rootPackage, serializer, initiator, acceptor) {
    init {
        object : Out(contractFile) {
            val type2id = LinkedHashMap<Class<*>, Int>()
            val visitedClasses = HashSet<Class<*>>()

            init {
                id2typeSerializer.forEach { (id, typeSerializer) ->
                    if (id >= FirstDescId) type2id[typeSerializer.type] = id
                }
                includeFile(includeFile)
                @Suppress("UNCHECKED_CAST") id2typeSerializer.values
                    .map { it.type }
                    .filter { it.isEnum }
                    .forEach { generateEnum(it as Class<Enum<*>>) }
                id2typeSerializer.values
                    .filter { it is FastSerializer.ClassTypeSerializer }
                    .forEach { generateClass(it.type) }
                interfaces.forEach { generateInterface(it) }
                generateServices(initiator, "initiator")
                generateServices(acceptor, "acceptor")
                tabs("export const SERIALIZER = new yass.FastSerializer(")
                inc()
                iterate(type2id.keys, { print(",") }, { type ->
                    println()
                    tabs(jsType(type, false))
                })
                println()
                dec()
                tabsln(");")
                close()
            }

            fun jsType(type: Class<*>, externalName: Boolean): String {
                val externalDesc = externalTypes[primitiveWrapperType(type)]
                if (externalDesc != null) return if (externalName) externalDesc.name else externalDesc.typeDescHolder
                checkType(type)
                check(!type.isArray) { "illegal type ${type.canonicalName} (use List instead [])" }
                return qualifiedName(type)
            }

            fun generateType(type: Class<*>, typeGenerator: (type: String) -> Unit) {
                checkType(type)
                val jsType = qualifiedName(type)
                val dot = jsType.lastIndexOf('.')
                val name = type.simpleName
                if (dot < 0) {
                    typeGenerator(name)
                } else {
                    tabsln("export namespace ${jsType.substring(0, dot)} {")
                    inc()
                    typeGenerator(name)
                    dec()
                    tabsln("}")
                }
                println()
            }

            fun generateEnum(type: Class<out Enum<*>>) = generateType(type) { name ->
                tabsln("export class $name extends yass.Enum {")
                inc()
                additionalTypeCode()
                for (e in type.enumConstants)
                    tabsln("static readonly ${e.name} = new $name(${e.ordinal}, '${e.name}');")
                tabsln("static readonly VALUES = <$name[]>yass.enumValues($name);")
                tabsln("static readonly TYPE_DESC = yass.enumDesc(${type2id[type]}, $name);")
                dec()
                tabsln("}")
            }

            fun type(type: Type?): String = when {
                type is ParameterizedType -> if (type.rawType === List::class.java) {
                    type(type.actualTypeArguments[0]) + "[]"
                } else {
                    val s = StringBuilder(type(type.rawType))
                    val actualTypeArguments = type.actualTypeArguments
                    if (actualTypeArguments.isNotEmpty()) {
                        s.append('<')
                        iterate(actualTypeArguments.asList(), { s.append(", ") }, { s.append(type(it)) })
                        s.append('>')
                    }
                    s.toString()
                }
                type is TypeVariable<*> -> type.name
                type === Double::class.javaPrimitiveType || type === Double::class.javaObjectType -> "number"
                type === Boolean::class.javaPrimitiveType || type === Boolean::class.javaObjectType -> "boolean"
                type === String::class.java -> "string"
                type === ByteArray::class.java -> "Uint8Array"
                isRootClass((type as Class<*>)) -> "any"
                type === Void.TYPE -> "void"
                else -> jsType(type, true)
            }

            fun typeDesc(fieldDesc: FieldDesc): String {
                val typeSerializer = fieldDesc.serializer.typeSerializer ?: return "null"
                return when {
                    ListTypeDesc.serializer === typeSerializer -> "yass.LIST_DESC"
                    BooleanDesc.serializer === typeSerializer -> "yass.BOOLEAN_DESC"
                    DoubleDesc.serializer === typeSerializer -> "yass.NUMBER_DESC"
                    StringDesc.serializer === typeSerializer -> "yass.STRING_DESC"
                    BinaryDesc.serializer === typeSerializer -> "yass.BYTES_DESC"
                    else -> jsType(typeSerializer.type, false) + ".TYPE_DESC"
                }
            }

            fun <C> generateClass(type: Class<C>) {
                if (!visitedClasses.add(type)) return
                var sc: Type? = type.genericSuperclass
                if (sc is Class<*>) {
                    if (isRootClass(sc)) sc = null else generateClass(sc)
                } else
                    generateClass((sc as ParameterizedType).rawType as Class<*>)
                val superClass = sc
                generateType(type) { name ->
                    tabs("export ${if (Modifier.isAbstract(type.modifiers)) "abstract " else ""}class $name")
                    val typeParameters = type.typeParameters
                    if (typeParameters.isNotEmpty()) {
                        print("<")
                        iterate(typeParameters.asList(), { print(", ") }, { print(it.name) })
                        print(">")
                    }
                    if (superClass != null) print(" extends ${type(superClass)}")
                    println(" {")
                    inc()
                    additionalTypeCode()
                    type.ownFields.forEach { tabsln("${it.name}: ${nullable(type(it.genericType))};") }
                    val id = type2id[type]
                    if (id != null) {
                        tabs("static readonly TYPE_DESC = yass.classDesc($id, $name")
                        inc()
                        val typeSerializer = id2typeSerializer[id] as FastSerializer.ClassTypeSerializer
                        check(!typeSerializer.graph) { "class '$type' is graph (not implemented in TypeScript)" }
                        for (fieldDesc in typeSerializer.fieldDescs) {
                            println(",")
                            tabs(
                                "new yass.FieldDesc(${fieldDesc.id}, '${fieldDesc.serializer.field.name}'," +
                                    " ${typeDesc(fieldDesc)})"
                            )
                        }
                        println()
                        dec()
                        tabsln(");")
                    }
                    dec()
                    tabsln("}")
                }
            }

            fun generateInterface(type: Class<*>) {
                SimpleMethodMapperFactory.invoke(type) // checks for overloaded methods (JavaScript restriction)
                val methods = getMethods(type)
                val methodMapper = methodMapper(type)
                generateType(type) { name ->
                    fun generateInterface(name: String, implementation: Boolean) {
                        tabsln("export namespace ${if (implementation) "impl" else "proxy"} {")
                        inc()
                        tabsln("export interface $name {")
                        inc()
                        for (method in methods) {
                            tabs("${method.name}(")
                            iterate(listOf(*method.parameters), { print(", ") }, { p ->
                                print("${p.name}: ${nullable(type(p.parameterizedType))}")
                            })
                            print("): ")
                            if (methodMapper.map(method).oneWay) {
                                print("void")
                            } else {
                                val t = nullable(type(method.genericReturnType))
                                if (implementation) print(t) else print("Promise<$t>")
                            }
                            println(";")
                        }
                        dec()
                        tabsln("}")
                        dec()
                        tabsln("}")
                    }
                    generateInterface(name, false)
                    generateInterface(name, true)
                    tabsln("export namespace mapper {")
                    inc()
                    tabs("export const $name = new yass.MethodMapper(")
                    inc()
                    iterate(methods, { print(",") }, { method ->
                        println()
                        val mapping = methodMapper.map(method)
                        tabs("new yass.MethodMapping('${mapping.method.name}', ${mapping.id}, ${mapping.oneWay})")
                    })
                    println()
                    dec()
                    tabsln(");")
                    dec()
                    tabsln("}")
                }
            }

            fun generateServices(services: Services?, role: String) {
                if (services == null) return
                tabsln("export namespace $role {")
                inc()
                for (serviceDesc in getServiceDescs(services)) {
                    var name = qualifiedName(serviceDesc.contractId.contract)
                    var namespace = ""
                    val dot = name.lastIndexOf('.') + 1
                    if (dot > 0) {
                        namespace = name.substring(0, dot)
                        name = name.substring(dot)
                    }
                    tabsln(
                        "export const ${serviceDesc.name} = new yass.ContractId<${namespace}proxy.$name, " +
                            "${namespace}impl.$name>(${serviceDesc.contractId.id}, ${namespace}mapper.$name);"
                    )
                }
                dec()
                tabsln("}")
                println()
            }

            fun additionalTypeCode() {
                if (additionalTypeCode != null) tabsln(additionalTypeCode)
            }
        }
    }
}
