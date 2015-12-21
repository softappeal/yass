package ch.softappeal.yass.ts;

import ch.softappeal.yass.Version;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.Services;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.serialize.fast.AbstractJsFastSerializer;
import ch.softappeal.yass.serialize.fast.ClassTypeHandler;
import ch.softappeal.yass.serialize.fast.FieldHandler;
import ch.softappeal.yass.serialize.fast.TypeDesc;
import ch.softappeal.yass.serialize.fast.TypeHandler;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public final class ContractGenerator extends Generator {

    private final String rootPackage;
    private final LinkedHashMap<Class<?>, Integer> type2id = new LinkedHashMap<>();
    private final SortedMap<Integer, TypeHandler> id2typeHandler;
    private final Set<Class<?>> visitedClasses = new HashSet<>();
    private final Map<Class<?>, String> java2tsBaseType = new HashMap<>();
    private final String contractModuleName;
    private MethodMapper.@Nullable Factory methodMapperFactory;

    private void checkType(final Class<?> type) {
        if (!type.getCanonicalName().startsWith(rootPackage)) {
            throw new RuntimeException("type '" + type.getCanonicalName() + "' doesn't have root package '" + rootPackage + "'");
        }
    }

    private String jsType(final Class<?> type) {
        final @Nullable String tsBaseType = java2tsBaseType.get(FieldHandler.primitiveWrapperType(type));
        if (tsBaseType != null) {
            return tsBaseType;
        }
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
            tabsln("export namespace %s {", jsType.substring(0, dot));
            inc();
            typeGenerator.generateType(name);
            dec();
            tabsln("}");
        }
        println();
    }

    private void generateEnum(final Class<? extends Enum<?>> type) {
        generateType(type, new TypeGenerator() {
            @Override public void generateType(final String name) {
                tabsln("export class %s extends yass.Enum {", name);
                inc();
                for (final Enum<?> e : type.getEnumConstants()) {
                    tabsln("static %s = new %s(%s, '%s');", e.name(), name, e.ordinal(), e.name());
                }
                tabsln("static TYPE_DESC = yass.enumDesc(%s, %s);", type2id.get(type), name);
                dec();
                tabsln("}");
            }
        });
    }

    private static final Set<Type> ROOT_CLASSES = new HashSet<>(Arrays.<Type>asList(
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
        } else if ((type == double.class) || (type == Double.class)) {
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
        return contractModuleName + jsType((Class<?>)type);
    }

    private String typeDescOwner(final ClassTypeHandler.FieldDesc fieldDesc) {
        final TypeHandler typeHandler = fieldDesc.handler.typeHandler();
        if (TypeDesc.LIST.handler == typeHandler) {
            return "yass.LIST_DESC";
        } else if (AbstractJsFastSerializer.BOOLEAN_TYPEDESC.handler == typeHandler) {
            return "yass.BOOLEAN_DESC";
        } else if (AbstractJsFastSerializer.DOUBLE_TYPEDESC.handler == typeHandler) {
            return "yass.NUMBER_DESC";
        } else if (AbstractJsFastSerializer.STRING_TYPEDESC.handler == typeHandler) {
            return "yass.STRING_DESC";
        } else if (AbstractJsFastSerializer.BYTES_TYPEDESC.handler == typeHandler) {
            return "yass.BYTES_DESC";
        } else if (typeHandler == null) {
            return "null";
        }
        return contractModuleName + jsType(typeHandler.type) + ".TYPE_DESC";
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
        generateType(type, new TypeGenerator() {
            @Override public void generateType(final String name) {
                tabsln(
                    "export %sclass %s extends %s {",
                    (Modifier.isAbstract(type.getModifiers()) ? "abstract " : ""), name, (superClass == null) ? "yass.Type" : (contractModuleName + jsType(superClass))
                );
                inc();
                for (final Field field : Reflect.ownFields(type)) {
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
            }
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

    private static List<ServiceDesc> getServiceDescs(final Services services) throws Exception {
        final List<ServiceDesc> serviceDescs = new ArrayList<>();
        for (final Field field : services.getClass().getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && (field.getType() == ContractId.class)) {
                serviceDescs.add(new ServiceDesc(field.getName(), (ContractId<?>)field.get(services)));
            }
        }
        Collections.sort(serviceDescs, new Comparator<ServiceDesc>() {
            @Override public int compare(final ServiceDesc serviceDesc1, final ServiceDesc serviceDesc2) {
                return ((Integer)serviceDesc1.contractId.id).compareTo((Integer)serviceDesc2.contractId.id);
            }
        });
        return serviceDescs;
    }

    private static Set<Class<?>> getInterfaces(final @Nullable Services services) throws Exception {
        if (services == null) {
            return new HashSet<>();
        }
        final Set<Class<?>> interfaces = new HashSet<>();
        for (final ServiceDesc serviceDesc : getServiceDescs(services)) {
            interfaces.add(serviceDesc.contractId.contract);
        }
        return interfaces;
    }

    private static Method[] getMethods(final Class<?> type) {
        final Method[] methods = type.getMethods();
        Arrays.sort(methods, new Comparator<Method>() {
            @Override public int compare(final Method method1, final Method method2) {
                return method1.getName().compareTo(method2.getName());
            }
        });
        return methods;
    }

    private void generateInterface(final Class<?> type) {
        checkType(type);
        final Method[] methods = getMethods(type);
        SimpleMethodMapper.FACTORY.create(type); // checks for overloaded methods (JavaScript restriction)
        final MethodMapper methodMapper = methodMapperFactory.create(type);
        generateType(type, new TypeGenerator() {
            void generateInterface(final String name, final boolean proxy) {
                tabsln("export interface %s {", name + (proxy ? "_PROXY" : ""));
                inc();
                for (final Method method : methods) {
                    tabs("%s(", method.getName());
                    int param = 0;
                    for (final Type paramType : method.getGenericParameterTypes()) {
                        if (param != 0) {
                            print(", ");
                        }
                        print("param%s: %s", param++, type(paramType));
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
                tabs("export const %s_MAPPER: yass.MethodMapper<%s> = new yass.MethodMapper<%s>(", name, name, name);
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

    private void generateServices(final @Nullable Services services, final String role) throws Exception {
        if (services == null) {
            return;
        }
        tabsln("export namespace %s {", role);
        inc();
        for (final ServiceDesc serviceDesc : getServiceDescs(services)) {
            final String name = contractModuleName + jsType(serviceDesc.contractId.contract);
            tabsln(
                "export const %s: yass.ContractId<%s, %s_PROXY> = new yass.ContractId<%s, %s_PROXY>(%s, %s_MAPPER);",
                serviceDesc.name, name, name, name, name, serviceDesc.contractId.id, name
            );
        }
        dec();
        tabsln("}");
        println();
    }

    /**
     * @param includePath path to base types or yass module
     */
    @SuppressWarnings("unchecked")
    public ContractGenerator(
        final Package rootPackage,
        final AbstractJsFastSerializer serializer,
        final @Nullable Services initiator,
        final @Nullable Services acceptor,
        final String includePath,
        final String contractModuleName,
        final @Nullable Map<Class<?>, String> java2tsBaseType,
        final String contractFilePath
    ) throws Exception {
        super(contractFilePath);
        this.rootPackage = rootPackage.getName() + '.';
        if (java2tsBaseType != null) {
            for (final Map.Entry<Class<?>, String> entry : java2tsBaseType.entrySet()) {
                this.java2tsBaseType.put(Check.notNull(entry.getKey()), Check.notNull(entry.getValue()));
            }
        }
        id2typeHandler = serializer.id2typeHandler();
        for (final Map.Entry<Integer, TypeHandler> entry : id2typeHandler.entrySet()) {
            final int id = entry.getKey();
            if (id >= AbstractJsFastSerializer.FIRST_ID) {
                type2id.put(entry.getValue().type, id);
            }
        }
        tabsln("/// <reference path='%s'/>", Check.notNull(includePath));
        println();
        this.contractModuleName = Check.notNull(contractModuleName) + '.';
        tabsln("namespace %s {", contractModuleName);
        println();
        inc();
        tabsln("export const GENERATED_BY_YASS_VERSION = '%s';", Version.VALUE);
        println();
        for (final TypeHandler typeHandler : id2typeHandler.values()) {
            final Class<?> type = typeHandler.type;
            if (type.isEnum()) {
                generateEnum((Class)type);
            }
        }
        for (final TypeHandler typeHandler : id2typeHandler.values()) {
            if (typeHandler instanceof ClassTypeHandler) {
                generateClass(typeHandler.type);
            }
        }
        if ((initiator != null) && (acceptor != null) && (initiator.methodMapperFactory != acceptor.methodMapperFactory)) {
            throw new IllegalArgumentException("initiator and acceptor must have same methodMapperFactory");
        }
        if (initiator != null) {
            methodMapperFactory = initiator.methodMapperFactory;
        }
        if (acceptor != null) {
            methodMapperFactory = acceptor.methodMapperFactory;
        }
        final Set<Class<?>> interfaceSet = getInterfaces(initiator);
        interfaceSet.addAll(getInterfaces(acceptor));
        final List<Class<?>> interfaceList = new ArrayList<>(interfaceSet);
        Collections.sort(interfaceList, new Comparator<Class<?>>() {
            @Override public int compare(final Class<?> type1, final Class<?> type2) {
                return type1.getCanonicalName().compareTo(type2.getCanonicalName());
            }
        });
        for (final Class<?> aInterface : interfaceList) {
            generateInterface(aInterface);
        }
        generateServices(initiator, "initiator");
        generateServices(acceptor, "acceptor");
        tabs("export const SERIALIZER = new yass.JsFastSerializer(");
        inc();
        boolean first = true;
        for (final Class<?> type : type2id.keySet()) {
            if (!first) {
                print(",");
            }
            first = false;
            println();
            tabs(this.contractModuleName + jsType(type));
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
