package ch.softappeal.yass.generate;

import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.Reflect;
import ch.softappeal.yass.remote.Services;
import ch.softappeal.yass.remote.SimpleMethodMapper;
import ch.softappeal.yass.serialize.fast.BaseTypeHandler;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.ClassTypeHandler;
import ch.softappeal.yass.serialize.fast.FastSerializer;
import ch.softappeal.yass.serialize.fast.FieldHandler;
import ch.softappeal.yass.serialize.fast.TypeDesc;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

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
        h.addAll(List.of(handlers));
        return h;
    }

    public static Collection<TypeDesc> baseTypeDescs(final TypeDesc... descs) {
        final List<TypeDesc> d = new ArrayList<>();
        d.add(BOOLEAN_DESC);
        d.add(DOUBLE_DESC);
        d.add(STRING_DESC);
        d.add(BYTES_DESC);
        d.addAll(List.of(descs));
        return d;
    }

    public static final class ExternalDesc {
        final String name;
        final String typeDescHolder;
        public ExternalDesc(final String name, final String typeDescHolder) {
            this.name = Objects.requireNonNull(name);
            this.typeDescHolder = Objects.requireNonNull(typeDescHolder);
        }
    }

    private static String nullable(final String type) {
        return "yass.Nullable<" + type + '>';
    }

    private final class TypeScriptOut extends Out {

        private final LinkedHashMap<Class<?>, Integer> type2id = new LinkedHashMap<>();
        private final Set<Class<?>> visitedClasses = new HashSet<>();
        private final Map<Class<?>, ExternalDesc> externalTypes = new HashMap<>();

        private String jsType(final Class<?> type, final boolean externalName) {
            final var externalDesc = externalTypes.get(FieldHandler.primitiveWrapperType(type));
            if (externalDesc != null) {
                return externalName ? externalDesc.name : externalDesc.typeDescHolder;
            }
            checkType(type);
            if (type.isArray()) {
                throw new IllegalArgumentException("illegal type " + type.getCanonicalName() + " (use List instead [])");
            }
            return qualifiedName(type);
        }

        private void generateType(final Class<?> type, final Consumer<String> typeGenerator) {
            checkType(type);
            final var jsType = qualifiedName(type);
            final var dot = jsType.lastIndexOf('.');
            final var name = type.getSimpleName();
            if (dot < 0) {
                typeGenerator.accept(name);
            } else {
                tabsln("export namespace %s {", jsType.substring(0, dot));
                inc();
                typeGenerator.accept(name);
                dec();
                tabsln("}");
            }
            println();
        }

        private void generateEnum(final Class<? extends Enum<?>> type) {
            generateType(type, name -> {
                tabsln("export class %s extends yass.Enum {", name);
                inc();
                addTypeKey();
                for (final Enum<?> e : type.getEnumConstants()) {
                    tabsln("static readonly %s = new %s(%s, '%s');", e.name(), name, e.ordinal(), e.name());
                }
                tabsln("static readonly VALUES = <%s[]>yass.enumValues(%s);", name, name);
                tabsln("static readonly TYPE_DESC = yass.enumDesc(%s, %s);", type2id.get(type), name);
                dec();
                tabsln("}");
            });
        }

        private String type(final Type type) {
            if (type instanceof ParameterizedType) {
                final var parameterizedType = (ParameterizedType)type;
                if (parameterizedType.getRawType() == List.class) {
                    return type(parameterizedType.getActualTypeArguments()[0]) + "[]";
                } else {
                    final var s = new StringBuilder(type(parameterizedType.getRawType()));
                    final var actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length != 0) {
                        s.append('<');
                        iterate(List.of(actualTypeArguments), () -> s.append(", "), actualTypeArgument -> s.append(type(actualTypeArgument)));
                        s.append('>');
                    }
                    return s.toString();
                }
            } else if (type instanceof TypeVariable) {
                return ((TypeVariable)type).getName();
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
            final var typeHandler = fieldDesc.handler.typeHandler().orElseThrow();
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

        private <C> void generateClass(final Class<C> type) {
            if (!visitedClasses.add(type)) {
                return;
            }
            var sc = type.getGenericSuperclass();
            if (sc instanceof Class) {
                if (isRootClass((Class<?>)sc)) {
                    sc = null;
                } else {
                    generateClass((Class<?>)sc);
                }
            } else {
                generateClass((Class<?>)((ParameterizedType)sc).getRawType());
            }
            final var superClass = sc;
            generateType(type, name -> {
                tabs("export %sclass %s", Modifier.isAbstract(type.getModifiers()) ? "abstract " : "", name);
                final var typeParameters = type.getTypeParameters();
                if (typeParameters.length != 0) {
                    print("<");
                    iterate(List.of(typeParameters), () -> print(", "), typeParameter -> print(typeParameter.getName()));
                    print(">");
                }
                if (superClass != null) {
                    print(" extends " + type(superClass));
                }
                println(" {");
                inc();
                addTypeKey();
                for (final var field : Reflect.ownFields(type)) {
                    tabsln("%s: %s;", field.getName(), nullable(type(field.getGenericType())));
                }
                final var id = type2id.get(type);
                if (id != null) {
                    tabs("static readonly TYPE_DESC = yass.classDesc(%s, %s", id, name);
                    inc();
                    final var typeHandler = (ClassTypeHandler)id2typeHandler.get(id);
                    if (typeHandler.referenceable) {
                        throw new IllegalArgumentException("class '" + type + "' is referenceable (not implemented in TypeScript)");
                    }
                    for (final var fieldDesc : typeHandler.fieldDescs()) {
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
            final var methods = getMethods(type);
            final var methodMapper = methodMapper(type);
            generateType(type, new Consumer<>() {
                private void generateInterface(final String name, final boolean implementation) {
                    tabsln("export namespace %s {", implementation ? "impl" : "proxy");
                    inc();
                    tabsln("export interface %s {", name);
                    inc();
                    for (final var method : methods) {
                        tabs("%s(", method.getName());
                        iterate(List.of(method.getParameters()), () -> print(", "), p -> print("%s: %s", p.getName(), nullable(type(p.getParameterizedType()))));
                        print("): ");
                        if (methodMapper.mapMethod(method).oneWay) {
                            print("void");
                        } else {
                            final var type = nullable(type(method.getGenericReturnType()));
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
                @Override public void accept(final String name) {
                    generateInterface(name, false);
                    generateInterface(name, true);
                    tabsln("export namespace mapper {");
                    inc();
                    tabs("export const %s = new yass.MethodMapper(", name);
                    inc();
                    iterate(List.of(methods), () -> print(","), method -> {
                        println();
                        final var mapping = methodMapper.mapMethod(method);
                        tabs("new yass.MethodMapping('%s', %s, %s)", mapping.method.getName(), mapping.id, mapping.oneWay);
                    });
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
            for (final var serviceDesc : getServiceDescs(services)) {
                var name = qualifiedName(serviceDesc.contractId.contract);
                var namespace = "";
                final var dot = name.lastIndexOf('.') + 1;
                if (dot > 0) {
                    namespace = name.substring(0, dot);
                    name = name.substring(dot);
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

        private final boolean addTypeKey;
        private void addTypeKey() {
            if (addTypeKey) {
                tabsln("protected readonly __TYPE_KEY__!: never;");
            }
        }

        @SuppressWarnings("unchecked")
        public TypeScriptOut(
            final String includeFile, final @Nullable Map<Class<?>, ExternalDesc> externalTypes, final String contractFile, final boolean addTypeKey
        ) throws Exception {
            super(contractFile);
            this.addTypeKey = addTypeKey;
            if (externalTypes != null) {
                externalTypes.forEach((java, ts) -> this.externalTypes.put(Objects.requireNonNull(java), Objects.requireNonNull(ts)));
            }
            id2typeHandler.forEach((id, typeHandler) -> {
                if (id >= FIRST_DESC_ID) {
                    type2id.put(typeHandler.type, id);
                }
            });
            includeFile(includeFile);
            id2typeHandler.values().stream().map(typeHandler -> typeHandler.type).filter(Class::isEnum).forEach(type -> generateEnum((Class<Enum<?>>)type));
            id2typeHandler.values().stream().filter(typeHandler -> typeHandler instanceof ClassTypeHandler).forEach(typeHandler -> generateClass(typeHandler.type));
            interfaces.forEach(this::generateInterface);
            generateServices(initiator, "initiator");
            generateServices(acceptor, "acceptor");
            tabs("export const SERIALIZER = new yass.FastSerializer(");
            inc();
            iterate(type2id.keySet(), () -> print(","), type -> {
                println();
                tabs(jsType(type, false));
            });
            println();
            dec();
            tabsln(");");
            close();
        }

    }

    /**
     * @param addTypeKey experimental feature (see <a href="https://github.com/softappeal/yass/pull/4">pull request</a>)
     */
    public TypeScriptGenerator(
        final String rootPackage, final FastSerializer serializer, final @Nullable Services initiator, final @Nullable Services acceptor,
        final String includeFile, final @Nullable Map<Class<?>, ExternalDesc> externalTypes, final String contractFile, final boolean addTypeKey
    ) throws Exception {
        super(rootPackage, serializer, initiator, acceptor);
        new TypeScriptOut(includeFile, externalTypes, contractFile, addTypeKey);
    }

    public TypeScriptGenerator(
        final String rootPackage, final FastSerializer serializer, final @Nullable Services initiator, final @Nullable Services acceptor,
        final String includeFile, final @Nullable Map<Class<?>, ExternalDesc> externalTypes, final String contractFile
    ) throws Exception {
        this(rootPackage, serializer, initiator, acceptor, includeFile, externalTypes, contractFile, false);
    }

}
