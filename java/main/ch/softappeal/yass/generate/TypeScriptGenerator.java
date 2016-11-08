package ch.softappeal.yass.generate;

import ch.softappeal.yass.Version;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.Services;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.serialize.fast.BaseTypeHandler;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.ClassTypeHandler;
import ch.softappeal.yass.serialize.fast.FastSerializer;
import ch.softappeal.yass.serialize.fast.FieldHandler;
import ch.softappeal.yass.serialize.fast.TypeDesc;
import ch.softappeal.yass.serialize.fast.TypeHandler;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * You must use the "-parameters" option for javac to get the real method parameter names.
 */
public final class TypeScriptGenerator extends Generator {

    public static final TypeDesc BOOLEAN_DESC = new TypeDesc(TypeDesc.FIRST_ID, BaseTypeHandlers.BOOLEAN);
    public static final TypeDesc DOUBLE_DESC = new TypeDesc(TypeDesc.FIRST_ID + 1, BaseTypeHandlers.DOUBLE);
    public static final TypeDesc STRING_DESC = new TypeDesc(TypeDesc.FIRST_ID + 2, BaseTypeHandlers.STRING);
    public static final TypeDesc BYTES_DESC = new TypeDesc(TypeDesc.FIRST_ID + 3, BaseTypeHandlers.BYTE_ARRAY);
    public static final int FIRST_DESC_ID = TypeDesc.FIRST_ID + 4;

    public static List<BaseTypeHandler<?>> baseTypeHandlers(final BaseTypeHandler<?>... handlers) {
        final List<BaseTypeHandler<?>> h = new ArrayList<>();
        h.add((BaseTypeHandler<?>)BOOLEAN_DESC.handler);
        h.add((BaseTypeHandler<?>)DOUBLE_DESC.handler);
        h.add((BaseTypeHandler<?>)STRING_DESC.handler);
        h.add((BaseTypeHandler<?>)BYTES_DESC.handler);
        h.addAll(Arrays.asList(handlers));
        return h;
    }

    public static Collection<TypeDesc> baseTypeDescs(final TypeDesc... descs) {
        final List<TypeDesc> d = new ArrayList<>();
        d.add(BOOLEAN_DESC);
        d.add(DOUBLE_DESC);
        d.add(STRING_DESC);
        d.add(BYTES_DESC);
        d.addAll(Arrays.asList(descs));
        return d;
    }

    public static final class ExternalDesc {
        final String name;
        final String typeDescHolder;
        public ExternalDesc(final String name, final String typeDescHolder) {
            this.name = Check.notNull(name);
            this.typeDescHolder = Check.notNull(typeDescHolder);
        }
    }

    @FunctionalInterface private interface TypeGenerator {
        void generateType(String name);
    }

    private final class TypeScriptOut extends Out {

        private final LinkedHashMap<Class<?>, Integer> type2id = new LinkedHashMap<>();
        private final Set<Class<?>> visitedClasses = new HashSet<>();
        private final Map<Class<?>, ExternalDesc> externalTypes = new HashMap<>();
        private final @Nullable String contractNamespace;

        private String jsType(final Class<?> type, final boolean externalName) {
            final @Nullable ExternalDesc externalDesc = externalTypes.get(FieldHandler.primitiveWrapperType(type));
            if (externalDesc != null) {
                return externalName ? externalDesc.name : externalDesc.typeDescHolder;
            }
            checkType(type);
            if (type.isArray()) {
                throw new IllegalArgumentException("illegal type " + type.getCanonicalName() + " (use List instead [])");
            }
            return (contractNamespace == null ? "" : contractNamespace) + qualifiedName(type);
        }

        private void generateType(final Class<?> type, final TypeGenerator typeGenerator) {
            checkType(type);
            final String jsType = qualifiedName(type);
            final int dot = jsType.lastIndexOf('.');
            final String name = type.getSimpleName();
            if (dot < 0) {
                typeGenerator.generateType(name);
            } else {
                tabsln("export namespace %s {", jsType.substring(0, dot));
                inc();
                typeGenerator.generateType(name);
                dec();
                tabsln("}");
            }
            println();
        }

        private void generateEnum(final Class<? extends Enum<?>> type) {
            generateType(type, name -> {
                tabsln("export class %s extends yass.Enum {", name);
                inc();
                for (final Enum<?> e : type.getEnumConstants()) {
                    tabsln("static readonly %s = new %s(%s, '%s');", e.name(), name, e.ordinal(), e.name());
                }
                tabsln("static readonly TYPE_DESC = yass.enumDesc(%s, %s);", type2id.get(type), name);
                dec();
                tabsln("}");
            });
        }

        private String type(final Type type) {
            if (type instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType)type;
                if (parameterizedType.getRawType() == List.class) {
                    return type(parameterizedType.getActualTypeArguments()[0]) + "[]";
                } else {
                    throw new RuntimeException("unexpected type '" + parameterizedType + '\'');
                }
            } else if ((type == double.class) || (type == Double.class)) {
                return "number";
            } else if ((type == boolean.class) || (type == Boolean.class)) {
                return "boolean";
            } else if (type == String.class) {
                return "string";
            } else if (type == byte[].class) {
                return "Uint8Array";
            } else if (isRootClass((Class<?>)type)) {
                return "any";
            } else if (type == void.class) {
                return "void";
            }
            return jsType((Class<?>)type, true);
        }

        private String typeDesc(final ClassTypeHandler.FieldDesc fieldDesc) {
            if (!fieldDesc.handler.typeHandler().isPresent()) {
                return "null";
            }
            final TypeHandler typeHandler = fieldDesc.handler.typeHandler().get();
            if (TypeDesc.LIST.handler == typeHandler) {
                return "yass.LIST_DESC";
            } else if (BOOLEAN_DESC.handler == typeHandler) {
                return "yass.BOOLEAN_DESC";
            } else if (DOUBLE_DESC.handler == typeHandler) {
                return "yass.NUMBER_DESC";
            } else if (STRING_DESC.handler == typeHandler) {
                return "yass.STRING_DESC";
            } else if (BYTES_DESC.handler == typeHandler) {
                return "yass.BYTES_DESC";
            }
            return jsType(typeHandler.type, false) + ".TYPE_DESC";
        }

        private void generateClass(final Class<?> type) {
            if (!visitedClasses.add(type)) {
                return;
            }
            @Nullable Class<?> sc = type.getSuperclass();
            if (isRootClass(sc)) {
                sc = null;
            } else {
                generateClass(sc);
            }
            final @Nullable Class<?> superClass = sc;
            generateType(type, name -> {
                tabsln(
                    "export %sclass %s%s {",
                    (Modifier.isAbstract(type.getModifiers()) ? "abstract " : ""), name, (superClass == null) ? "" : (" extends " + jsType(superClass, true))
                );
                inc();
                for (final Field field : Reflect.ownFields(type)) {
                    tabsln("%s: %s;", field.getName(), type(field.getGenericType()));
                }
                final @Nullable Integer id = type2id.get(type);
                if (id != null) {
                    tabs("static readonly TYPE_DESC = yass.classDesc(%s, %s", id, name);
                    inc();
                    final ClassTypeHandler typeHandler = (ClassTypeHandler)id2typeHandler.get(id);
                    if (typeHandler.referenceable) {
                        throw new IllegalArgumentException("class '" + type + "' is referenceable (not implemented in TypeScript)");
                    }
                    for (final ClassTypeHandler.FieldDesc fieldDesc : typeHandler.fieldDescs()) {
                        println(",");
                        tabs("new yass.FieldDesc(%s, '%s', %s)", fieldDesc.id, fieldDesc.handler.field.getName(), typeDesc(fieldDesc));
                    }
                    println();
                    dec();
                    tabsln(");");
                }
                dec();
                tabsln("}");
            });
        }

        private void generateInterface(final Class<?> type) {
            SimpleMethodMapper.FACTORY.create(type); // checks for overloaded methods (JavaScript restriction)
            final Method[] methods = getMethods(type);
            final MethodMapper methodMapper = methodMapper(type);
            generateType(type, new TypeGenerator() {
                void generateInterface(final String name, final boolean implementation) {
                    tabsln("export namespace %s {", implementation ? "impl" : "proxy");
                    inc();
                    tabsln("export interface %s {", name);
                    inc();
                    for (final Method method : methods) {
                        tabs("%s(", method.getName());
                        boolean first = true;
                        for (final Parameter parameter : method.getParameters()) {
                            if (!first) {
                                print(", ");
                            }
                            first = false;
                            print("%s: %s", parameter.getName(), type(parameter.getParameterizedType()));
                        }
                        print("): ");
                        if (methodMapper.mapMethod(method).oneWay) {
                            print("void");
                        } else {
                            final String type = type(method.getGenericReturnType());
                            if (implementation) {
                                print(type);
                            } else {
                                print("Promise<%s>", type);
                            }
                        }
                        println(";");
                    }
                    dec();
                    tabsln("}");
                    dec();
                    tabsln("}");
                }
                @Override public void generateType(final String name) {
                    generateInterface(name, false);
                    generateInterface(name, true);
                    tabsln("export namespace mapper {");
                    inc();
                    tabs("export const %s = new yass.MethodMapper(", name);
                    inc();
                    boolean first = true;
                    for (final Method method : methods) {
                        if (!first) {
                            print(",");
                        }
                        first = false;
                        println();
                        final MethodMapper.Mapping mapping = methodMapper.mapMethod(method);
                        tabs("new yass.MethodMapping('%s', %s, %s)", mapping.method.getName(), mapping.id, mapping.oneWay);
                    }
                    println();
                    dec();
                    tabsln(");");
                    dec();
                    tabsln("}");
                }
            });
        }

        private void generateServices(final @Nullable Services services, final String role) throws Exception {
            if (services == null) {
                return;
            }
            tabsln("export namespace %s {", role);
            inc();
            for (final ServiceDesc serviceDesc : getServiceDescs(services)) {
                String name = qualifiedName(serviceDesc.contractId.contract);
                String namespace = "";
                final int dot = name.lastIndexOf('.') + 1;
                if (dot > 0) {
                    namespace = name.substring(0, dot);
                    name = name.substring(dot);
                }
                if (contractNamespace != null) {
                    namespace = contractNamespace + namespace;
                }
                tabsln(
                    "export const %s = new yass.ContractId<%sproxy.%s, %simpl.%s>(%s, %smapper.%s);",
                    serviceDesc.name, namespace, name, namespace, name, serviceDesc.contractId.id, namespace, name
                );
            }
            dec();
            tabsln("}");
            println();
        }

        @SuppressWarnings("unchecked")
        public TypeScriptOut(final String includeFile, final @Nullable String contractNamespace, final @Nullable Map<Class<?>, ExternalDesc> externalTypes, final String contractFile) throws Exception {
            super(contractFile);
            if (externalTypes != null) {
                externalTypes.forEach((java, ts) -> this.externalTypes.put(Check.notNull(java), Check.notNull(ts)));
            }
            id2typeHandler.forEach((id, typeHandler) -> {
                if (id >= FIRST_DESC_ID) {
                    type2id.put(typeHandler.type, id);
                }
            });
            includeFile(includeFile);
            this.contractNamespace = contractNamespace == null ? null : contractNamespace + '.';
            if (this.contractNamespace != null) {
                tabsln("namespace %s {", contractNamespace);
                println();
                inc();
            }
            tabsln("export const GENERATED_BY_YASS_VERSION = '%s';", Version.VALUE);
            println();
            id2typeHandler.values().stream().map(typeHandler -> typeHandler.type).filter(Class::isEnum).forEach(type -> generateEnum((Class<Enum<?>>)type));
            id2typeHandler.values().stream().filter(typeHandler -> typeHandler instanceof ClassTypeHandler).forEach(typeHandler -> generateClass(typeHandler.type));
            interfaces.forEach(this::generateInterface);
            generateServices(initiator, "initiator");
            generateServices(acceptor, "acceptor");
            tabs("export const SERIALIZER = new yass.FastSerializer(");
            inc();
            boolean first = true;
            for (final Class<?> type : type2id.keySet()) {
                if (!first) {
                    print(",");
                }
                first = false;
                println();
                tabs(jsType(type, false));
            }
            println();
            dec();
            tabsln(");");
            if (this.contractNamespace != null) {
                dec();
                println();
                tabsln("}");
            }
            close();
        }

    }

    /**
     * @param contractNamespace if null generate module else namespace
     */
    public TypeScriptGenerator(
        final String rootPackage, final FastSerializer serializer, final @Nullable Services initiator, final @Nullable Services acceptor,
        final String includeFile, final @Nullable String contractNamespace, final @Nullable Map<Class<?>, ExternalDesc> externalTypes, final String contractFile
    ) throws Exception {
        super(rootPackage, serializer, initiator, acceptor);
        new TypeScriptOut(includeFile, contractNamespace, externalTypes, contractFile);
    }

    public TypeScriptGenerator(
        final String rootPackage, final FastSerializer serializer, final @Nullable Services initiator, final @Nullable Services acceptor,
        final String includeFile, final @Nullable Map<Class<?>, ExternalDesc> externalTypes, final String contractFile
    ) throws Exception {
        this(rootPackage, serializer, initiator, acceptor, includeFile, null, externalTypes, contractFile);
    }

}
