package ch.softappeal.yass.ts;

import ch.softappeal.yass.Version;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.serialize.fast.ClassTypeHandler;
import ch.softappeal.yass.serialize.fast.JsFastSerializer;
import ch.softappeal.yass.serialize.fast.TypeDesc;
import ch.softappeal.yass.serialize.fast.TypeHandler;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * You must use the "-parameters" option for javac to get the real method parameter names.
 */
public final class ContractGenerator extends Generator {

    private final String rootPackage;
    private final LinkedHashMap<Class<?>, Integer> type2id = new LinkedHashMap<>();
    private final SortedMap<Integer, TypeHandler> id2typeHandler;
    private final Set<Class<?>> visitedClasses = new HashSet<>();
    private final MethodMapper.Factory methodMapperFactory;

    private void checkType(final Class<?> type) {
        if (!type.getCanonicalName().startsWith(rootPackage)) {
            throw new RuntimeException("type '" + type.getCanonicalName() + "' doesn't have root package '" + rootPackage + "'");
        }
    }

    private String jsType(final Class<?> type) {
        checkType(type);
        return type.getCanonicalName().substring(rootPackage.length());
    }

    private interface TypeGenerator {
        void generateType(String name);
    }

    private void generateType(final Class<?> type, final TypeGenerator typeGenerator) {
        final String jsType = jsType(type);
        final int dot = jsType.lastIndexOf('.');
        final String name = type.getSimpleName();
        if (dot < 0) {
            typeGenerator.generateType(name);
        } else {
            tabsln("export module %s {", jsType.substring(0, dot));
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
                tabsln("static %s = new %s(%s, '%s');", e.name(), name, e.ordinal(), e.name());
            }
            tabsln("static TYPE_DESC = yass.enumDesc(%s, %s);", type2id.get(type), name);
            dec();
            tabsln("}");
        });
    }

    private static final Set<Type> ROOT_CLASSES = new HashSet<>(Arrays.asList(
        Object.class,
        Exception.class,
        RuntimeException.class,
        Error.class,
        Throwable.class
    ));

    private String type(final Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType)type;
            if (parameterizedType.getRawType() == List.class) {
                return type(parameterizedType.getActualTypeArguments()[0]) + "[]";
            } else {
                throw new RuntimeException("unexpected type '" + parameterizedType + '\'');
            }
        } else if ((type == int.class) || (type == Integer.class)) {
            return "number";
        } else if ((type == boolean.class) || (type == Boolean.class)) {
            return "boolean";
        } else if (type == String.class) {
            return "string";
        } else if (type == byte[].class) {
            return "Uint8Array";
        } else if (ROOT_CLASSES.contains(type)) {
            return "any";
        } else if (type == void.class) {
            return "void";
        }
        return jsType((Class<?>)type);
    }

    private String typeDescOwner(final ClassTypeHandler.FieldDesc fieldDesc) {
        final TypeHandler typeHandler = fieldDesc.handler.typeHandler();
        if (TypeDesc.LIST.handler == typeHandler) {
            return "yass.LIST_DESC";
        } else if (JsFastSerializer.BOOLEAN_TYPEDESC.handler == typeHandler) {
            return "yass.BOOLEAN_DESC";
        } else if (JsFastSerializer.INTEGER_TYPEDESC.handler == typeHandler) {
            return "yass.INTEGER_DESC";
        } else if (JsFastSerializer.STRING_TYPEDESC.handler == typeHandler) {
            return "yass.STRING_DESC";
        } else if (JsFastSerializer.BYTES_TYPEDESC.handler == typeHandler) {
            return "yass.BYTES_DESC";
        } else if (typeHandler == null) {
            return "null";
        }
        return jsType(typeHandler.type) + ".TYPE_DESC";
    }

    private void generateClass(final Class<?> type) {
        if (!visitedClasses.add(type)) {
            return;
        }
        Class<?> sc = type.getSuperclass();
        if (ROOT_CLASSES.contains(sc)) {
            sc = null;
        } else {
            generateClass(sc);
        }
        final Class<?> superClass = sc;
        generateType(type, name -> {
            final List<Field> fields = Reflect.ownFields(type);
            tabsln("export class %s extends %s {", name, (superClass == null) ? "yass.Type" : jsType(superClass));
            inc();
            for (final Field field : fields) {
                tabsln("%s: %s;", field.getName(), type(field.getGenericType()));
            }
            final Integer id = type2id.get(type);
            if (id != null) {
                tabs("static TYPE_DESC = yass.classDesc(%s, %s", id, name);
                inc();
                for (final ClassTypeHandler.FieldDesc fieldDesc : ((ClassTypeHandler)id2typeHandler.get(id)).fieldDescs()) {
                    println(",");
                    tabs("new yass.FieldDesc(%s, '%s', %s)", fieldDesc.id, fieldDesc.handler.field.getName(), typeDescOwner(fieldDesc));
                }
                println();
                dec();
                tabsln(");");
            }
            dec();
            tabsln("}");
        });
    }

    private static final class ServiceDesc {
        final String name;
        final ContractId<?> contractId;
        ServiceDesc(final String name, final ContractId<?> contractId) {
            this.name = name;
            this.contractId = contractId;
        }
    }

    private static List<ServiceDesc> getServiceDescs(final Class<?> services) throws Exception {
        final List<ServiceDesc> serviceDescs = new ArrayList<>();
        for (final Field field : services.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && (field.getType() == ContractId.class)) {
                serviceDescs.add(new ServiceDesc(field.getName(), (ContractId<?>)field.get(null)));
            }
        }
        Collections.sort(
            serviceDescs,
            (serviceDesc1, serviceDesc2) -> ((Integer)serviceDesc1.contractId.id).compareTo((Integer)serviceDesc2.contractId.id)
        );
        return serviceDescs;
    }

    private static Set<Class<?>> getInterfaces(final Class<?> services) throws Exception {
        return getServiceDescs(services).stream().map(serviceDesc -> serviceDesc.contractId.contract).collect(Collectors.toSet());
    }

    private static Method[] getMethods(final Class<?> type) {
        final Method[] methods = type.getMethods();
        Arrays.sort(methods, (method1, method2) -> method1.getName().compareTo(method2.getName()));
        return methods;
    }

    private void generateInterface(final Class<?> type) {
        checkType(type);
        final Method[] methods = getMethods(type);
        final MethodMapper methodMapper = methodMapperFactory.create(type);
        generateType(type, new TypeGenerator() {
            void generateInterface(final String name, final boolean proxy) {
                tabsln("export interface %s {", name + (proxy ? "_PROXY" : ""));
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
                        if (proxy) {
                            print("Promise<%s>", type);
                        } else {
                            print(type);
                        }
                    }
                    println(";");
                }
                dec();
                tabsln("}");
            }
            @Override public void generateType(final String name) {
                generateInterface(name, false);
                generateInterface(name, true);
                tabs("export var %s_MAPPER = new yass.MethodMapper<%s>(", name, name);
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
            }
        });
    }

    private void generateServices(final Class<?> services) throws Exception {
        tabsln("export module %s {", jsType(services));
        inc();
        for (final ServiceDesc serviceDesc : getServiceDescs(services)) {
            final String name = jsType(serviceDesc.contractId.contract);
            tabsln("export var %s = new yass.ContractId<%s, %s_PROXY>(%s, %s_MAPPER);", serviceDesc.name, name, name, serviceDesc.contractId.id, name);
        }
        dec();
        tabsln("}");
        println();
    }

    public static final String CLIENT_SERVICES = "ClientServices";
    public static final String SERVER_SERVICES = "ServerServices";

    /**
     * @param rootPackage Must contain the classes {@link #CLIENT_SERVICES} and {@link #SERVER_SERVICES} with static fields of type {@link ContractId}.
     * @param methodMapperFactory You must provide a factory that doesn't allow overloading due to JavaScript restrictions.
     */
    @SuppressWarnings("unchecked")
    public ContractGenerator(
        final Package rootPackage,
        final JsFastSerializer serializer,
        final MethodMapper.Factory methodMapperFactory,
        final String baseTypesModulePath,
        final String contractModuleName,
        final String contractFilePath
    ) throws Exception {
        super(contractFilePath);
        this.rootPackage = rootPackage.getName() + '.';
        this.methodMapperFactory = Check.notNull(methodMapperFactory);
        id2typeHandler = serializer.id2typeHandler();
        id2typeHandler.forEach((id, typeHandler) -> {
            if (id >= JsFastSerializer.FIRST_ID) {
                type2id.put(typeHandler.type, id);
            }
        });
        tabsln("/// <reference path='%s'/>", Check.notNull(baseTypesModulePath));
        println();
        tabsln("module %s {", Check.notNull(contractModuleName));
        println();
        inc();
        tabsln("export var GENERATED_BY_YASS_VERSION = '%s';", Version.VALUE);
        println();
        id2typeHandler.values().stream().map(typeHandler -> typeHandler.type).filter(Class::isEnum).forEach(type -> generateEnum((Class<Enum<?>>)type));
        id2typeHandler.values().stream().filter(typeHandler -> typeHandler instanceof ClassTypeHandler).forEach(typeHandler -> generateClass(typeHandler.type));
        final Class<?> clientServices = Class.forName(this.rootPackage + CLIENT_SERVICES);
        final Class<?> serverServices = Class.forName(this.rootPackage + SERVER_SERVICES);
        final Set<Class<?>> interfaceSet = getInterfaces(clientServices);
        interfaceSet.addAll(getInterfaces(serverServices));
        final List<Class<?>> interfaceList = new ArrayList<>(interfaceSet);
        Collections.sort(interfaceList, (type1, type2) -> type1.getCanonicalName().compareTo(type2.getCanonicalName()));
        interfaceList.forEach(this::generateInterface);
        generateServices(clientServices);
        generateServices(serverServices);
        tabs("export var SERIALIZER = new yass.JsFastSerializer(");
        inc();
        boolean first = true;
        for (final Class<?> type : type2id.keySet()) {
            if (!first) {
                print(",");
            }
            first = false;
            println();
            tabs(jsType(type));
        }
        println();
        dec();
        tabsln(");");
        dec();
        println();
        tabsln("}");
        close();
    }

}
