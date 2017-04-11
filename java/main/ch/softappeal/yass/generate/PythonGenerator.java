package ch.softappeal.yass.generate;

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
import ch.softappeal.yass.util.Exceptions;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * You must use the "-parameters" option for javac to get the real method parameter names.
 */
public final class PythonGenerator extends Generator {

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

    private static final String INIT_PY = "/__init__.py";
    private static final String ROOT_MODULE = "contract";

    public static final class ExternalDesc {
        final String name;
        final String typeDesc;
        public ExternalDesc(final String name, final String typeDesc) {
            this.name = Objects.requireNonNull(name);
            this.typeDesc = Objects.requireNonNull(typeDesc);
        }
    }

    private final boolean python3;
    private final @Nullable String includeFileForEachModule;
    private final Map<String, String> module2includeFile = new HashMap<>();
    private final SortedMap<Class<?>, ExternalDesc> externalTypes = new TreeMap<>(Comparator.comparing(Class::getCanonicalName));
    private final Namespace rootNamespace = new Namespace(null, null, ROOT_MODULE, 0);
    private final LinkedHashMap<Class<?>, Namespace> type2namespace = new LinkedHashMap<>();
    private final Map<Class<?>, Integer> type2id = new HashMap<>();

    private final class Namespace {
        final @Nullable Namespace parent;
        final @Nullable String name;
        final String moduleName;
        final int depth;
        final LinkedHashSet<Class<?>> types = new LinkedHashSet<>();
        private final Map<String, Namespace> children = new HashMap<>();
        Namespace(final @Nullable Namespace parent, final @Nullable String name, final String moduleName, final int depth) {
            this.parent = parent;
            this.name = name;
            this.moduleName = Objects.requireNonNull(moduleName);
            this.depth = depth;
        }
        private void add(final String qualifiedName, final Class<?> type) {
            final int dot = qualifiedName.indexOf('.');
            if (dot < 0) { // leaf
                checkType(type);
                if (!type.isEnum() && !type.isInterface()) { // note: baseclasses must be before subclasses
                    final Class<?> superClass = type.getSuperclass();
                    if (!isRootClass(superClass)) {
                        rootNamespace.add(superClass);
                    }
                }
                types.add(type);
                type2namespace.put(type, this);
            } else { // intermediate
                children.computeIfAbsent(
                    qualifiedName.substring(0, dot),
                    name -> new Namespace(this, name, moduleName + '_' + name, depth + 1)
                ).add(qualifiedName.substring(dot + 1), type);
            }
        }
        void add(final Class<?> type) {
            add(qualifiedName(type), type);
        }
        void generate(final String path) {
            try {
                new ContractPythonOut(Objects.requireNonNull(path) + INIT_PY, this);
            } catch (final Exception e) {
                throw Exceptions.wrap(e);
            }
            children.forEach((name, namespace) -> namespace.generate(path + '/' + name));
        }
    }

    public PythonGenerator(
        final String rootPackage, final FastSerializer serializer, final @Nullable Services initiator, final @Nullable Services acceptor, final boolean python3,
        final @Nullable String includeFileForEachModule, final @Nullable Map<String, String> module2includeFile, final @Nullable Map<Class<?>, ExternalDesc> externalTypes, final String generatedDir
    ) throws Exception {
        super(rootPackage, serializer, initiator, acceptor);
        this.python3 = python3;
        this.includeFileForEachModule = includeFileForEachModule;
        if (module2includeFile != null) {
            module2includeFile.forEach((m, i) -> this.module2includeFile.put(Objects.requireNonNull(m), Objects.requireNonNull(i)));
        }
        if (externalTypes != null) {
            externalTypes.forEach((java, py) -> this.externalTypes.put(Objects.requireNonNull(java), Objects.requireNonNull(py)));
        }
        id2typeHandler.forEach((id, typeHandler) -> {
            if (id >= FIRST_DESC_ID) {
                final Class<?> type = typeHandler.type;
                type2id.put(type, id);
                if (!this.externalTypes.containsKey(type)) {
                    rootNamespace.add(type);
                }
            }
        });
        interfaces.forEach(rootNamespace::add);
        rootNamespace.generate(generatedDir + '/' + ROOT_MODULE);
        new MetaPythonOut(generatedDir + INIT_PY);
    }

    private TypeHandler typeHandler(final Class<?> type) {
        return Objects.requireNonNull(id2typeHandler.get(Objects.requireNonNull(type2id.get(Objects.requireNonNull(type)))));
    }

    private static String pyBool(final boolean value) {
        return value ? "True" : "False";
    }

    private boolean hasClassDesc(final Class<?> type) {
        return !type.isEnum() && !Modifier.isAbstract(type.getModifiers()) && (typeHandler(type) instanceof ClassTypeHandler);
    }

    private final class ContractPythonOut extends Out {
        private final Namespace namespace;
        private final SortedSet<Namespace> modules = new TreeSet<>(Comparator.comparing(n -> n.moduleName));
        private String getQualifiedName(final Class<?> type) {
            final Namespace ns = Objects.requireNonNull(type2namespace.get(Objects.requireNonNull(type)));
            modules.add(ns);
            return ((namespace != ns) ? ns.moduleName + '.' : "") + type.getSimpleName();
        }
        private void importModule(final Namespace module) {
            print("from ");
            for (int d = namespace.depth + 2; d > 0; d--) {
                print(".");
            }
            if (module == rootNamespace) {
                println(" import " + ROOT_MODULE);
            } else {
                println("%s import %s as %s", module.parent.moduleName.replace('_', '.'), module.name, module.moduleName);
            }
        }
        @SuppressWarnings("unchecked") ContractPythonOut(final String file, final Namespace namespace) throws Exception {
            super(file);
            this.namespace = Objects.requireNonNull(namespace);
            println("from enum import Enum");
            println("from typing import List, Any, cast");
            println();
            println("import yass");
            if (includeFileForEachModule != null) {
                includeFile(includeFileForEachModule);
            }
            final @Nullable String moduleIncludeFile = module2includeFile.get(
                (namespace == rootNamespace) ? "" : namespace.moduleName.substring(ROOT_MODULE.length() + 1).replace('_', '.')
            );
            if (moduleIncludeFile != null) {
                println2();
                includeFile(moduleIncludeFile);
            }
            final StringBuilder buffer = new StringBuilder();
            redirect(buffer);
            namespace.types.stream().filter(Class::isEnum).forEach(type -> generateEnum((Class<Enum<?>>)type));
            namespace.types.stream()
                .filter(t -> !t.isEnum() && !t.isInterface() && (Modifier.isAbstract(t.getModifiers()) || (typeHandler(t) instanceof ClassTypeHandler)))
                .forEach(this::generateClass);
            namespace.types.stream().filter(Class::isInterface).forEach(this::generateInterface);
            redirect(null);
            modules.stream().filter(module -> module != namespace).forEach(this::importModule);
            print(buffer);
            close();
        }
        private void generateEnum(final Class<? extends Enum<?>> type) {
            println2();
            tabsln("class %s(Enum):", type.getSimpleName());
            for (final Enum<?> e : type.getEnumConstants()) {
                tab();
                tabsln("%s = %s", e.name(), e.ordinal());
            }
        }
        private String pythonType(final Type type) {
            if (type instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType)type;
                if (parameterizedType.getRawType() == List.class) {
                    return "List[" + pythonType(parameterizedType.getActualTypeArguments()[0]) + ']';
                } else {
                    throw new RuntimeException("unexpected type '" + parameterizedType + '\'');
                }
            } else if (type == void.class) {
                return "None";
            } else if ((type == double.class) || (type == Double.class)) {
                return "float";
            } else if ((type == boolean.class) || (type == Boolean.class)) {
                return "bool";
            } else if (type == String.class) {
                return python3 ? "str" : "unicode";
            } else if (type == byte[].class) {
                return "bytes";
            } else if (type == Object.class) {
                return "Any";
            } else if (Throwable.class.isAssignableFrom((Class<?>)type)) {
                return "Exception";
            }
            final Class<?> t = (Class<?>)type;
            final @Nullable ExternalDesc externalDesc = externalTypes.get(FieldHandler.primitiveWrapperType(t));
            if (externalDesc != null) {
                return externalDesc.name;
            }
            checkType(t);
            if (t.isArray()) {
                throw new IllegalArgumentException("illegal type " + t.getCanonicalName() + " (use List instead [])");
            }
            return getQualifiedName(t);
        }
        private void generateClass(final Class<?> type) {
            println2();
            final Class<?> sc = type.getSuperclass();
            final @Nullable Class<?> superClass = isRootClass(sc) ? null : sc;
            if (Modifier.isAbstract(type.getModifiers())) {
                println("@yass.abstract");
            }
            tabs("class %s", type.getSimpleName());
            boolean hasSuper = false;
            if (superClass != null) {
                hasSuper = true;
                print("(%s)", getQualifiedName(superClass));
            } else if (Throwable.class.isAssignableFrom(sc)) {
                print("(Exception)");
            }
            println(":");
            inc();
            tabsln("def __init__(self)%s", python3 ? " -> None:" : ":  # type: () -> None");
            inc();
            if (hasSuper) {
                tabsln("%s.__init__(self)", getQualifiedName(superClass));
            }
            final List<Field> ownFields = Reflect.ownFields(type);
            if (ownFields.isEmpty() && !hasSuper) {
                tabsln("pass");
            } else {
                for (final Field field : ownFields) {
                    final String t = pythonType(field.getGenericType());
                    if (python3) {
                        tabsln("self.%s: %s = cast(%s, None)", field.getName(), t, t);
                    } else {
                        tabsln("self.%s = cast('%s', None)  # type: %s", field.getName(), t, t);
                    }
                }
            }
            dec();
            dec();
        }
        private void generateInterface(final Class<?> type) {
            SimpleMethodMapper.FACTORY.create(type); // checks for overloaded methods (Python restriction)
            final MethodMapper methodMapper = methodMapper(type);
            println2();
            println("class %s:", type.getSimpleName());
            inc();
            boolean first = true;
            for (final Method method : getMethods(type)) {
                if (first) {
                    first = false;
                } else {
                    println();
                }
                tabs("def %s(self", method.getName());
                if (python3) {
                    for (final Parameter parameter : method.getParameters()) {
                        print(", %s: %s", parameter.getName(), pythonType(parameter.getParameterizedType()));
                    }
                    println(") -> %s:", methodMapper.mapMethod(method).oneWay ? "None" : pythonType(method.getGenericReturnType()));
                } else {
                    for (final Parameter parameter : method.getParameters()) {
                        print(", %s", parameter.getName());
                    }
                    print("):  # type: (");
                    boolean firstType = true;
                    for (final Parameter parameter : method.getParameters()) {
                        if (firstType) {
                            firstType = false;
                        } else {
                            print(", ");
                        }
                        print(pythonType(parameter.getParameterizedType()));
                    }
                    println(") -> %s", methodMapper.mapMethod(method).oneWay ? "None" : pythonType(method.getGenericReturnType()));
                }
                tab();
                tabsln("raise NotImplementedError()");
            }
            dec();
        }
    }

    private final class MetaPythonOut extends Out {
        private String getQualifiedName(final Class<?> type) {
            final Namespace ns = Objects.requireNonNull(type2namespace.get(Objects.requireNonNull(type)));
            return ns.moduleName + '.' + type.getSimpleName();
        }
        private void importModule(final Namespace module) {
            print("from .");
            if (module == rootNamespace) {
                println(" import " + ROOT_MODULE);
            } else {
                println("%s import %s as %s", module.parent.moduleName.replace('_', '.'), module.name, module.moduleName);
            }
        }
        MetaPythonOut(final String file) throws Exception {
            super(file);
            println("import yass");
            if (includeFileForEachModule != null) {
                includeFile(includeFileForEachModule);
            }
            new LinkedHashSet<>(type2namespace.values()).forEach(this::importModule);
            println();
            type2namespace.keySet().forEach(type -> {
                final String qn = getQualifiedName(type);
                if (type.isEnum()) {
                    println("yass.enumDesc(%s, %s)", type2id.get(type), qn);
                } else if (hasClassDesc(type)) {
                    println("yass.classDesc(%s, %s, %s)", type2id.get(type), qn, pyBool(((ClassTypeHandler)typeHandler(type)).referenceable));
                }
            });
            println();
            type2namespace.keySet().stream().filter(PythonGenerator.this::hasClassDesc).forEach(type -> {
                tabsln("yass.fieldDescs(%s, [", getQualifiedName(type));
                for (final ClassTypeHandler.FieldDesc fieldDesc : ((ClassTypeHandler)typeHandler(type)).fieldDescs()) {
                    tab();
                    tabsln("yass.FieldDesc(%s, '%s', %s),", fieldDesc.id, fieldDesc.handler.field.getName(), typeDesc(fieldDesc));
                }
                tabsln("])");
            });
            println();
            println("SERIALIZER = yass.FastSerializer([");
            inc();
            externalTypes.values().forEach(externalDesc -> tabsln("%s,", externalDesc.typeDesc));
            type2namespace.keySet().stream().filter(t -> !Modifier.isAbstract(t.getModifiers())).forEach(t -> tabsln("%s,", getQualifiedName(t)));
            dec();
            println("])");
            println();
            interfaces.forEach(this::generateMapper);
            generateServices(initiator, "INITIATOR");
            generateServices(acceptor, "ACCEPTOR");
            close();
        }
        private void generateServices(final @Nullable Services services, final String role) throws Exception {
            if (services == null) {
                return;
            }
            println2();
            tabsln("class %s:", role);
            for (final ServiceDesc sd : getServiceDescs(services)) {
                final String qn = getQualifiedName(sd.contractId.contract);
                tab();
                if (python3) {
                    tabsln("%s: yass.ContractId[%s] = yass.ContractId(%s, %s)", sd.name, qn, qn, sd.contractId.id);
                } else {
                    tabsln("%s = yass.ContractId(%s, %s)  # type: yass.ContractId[%s]", sd.name, qn, sd.contractId.id, qn);
                }
            }
        }
        private String typeDesc(final ClassTypeHandler.FieldDesc fieldDesc) {
            if (!fieldDesc.handler.typeHandler().isPresent()) {
                return "None";
            }
            final TypeHandler typeHandler = fieldDesc.handler.typeHandler().get();
            if (TypeDesc.LIST.handler == typeHandler) {
                return "yass.LIST_DESC";
            } else if (BOOLEAN_DESC.handler == typeHandler) {
                return "yass.BOOLEAN_DESC";
            } else if (DOUBLE_DESC.handler == typeHandler) {
                return "yass.DOUBLE_DESC";
            } else if (STRING_DESC.handler == typeHandler) {
                return "yass.STRING_DESC";
            } else if (BYTES_DESC.handler == typeHandler) {
                return "yass.BYTES_DESC";
            }
            final @Nullable ExternalDesc externalDesc = externalTypes.get(FieldHandler.primitiveWrapperType(typeHandler.type));
            if (externalDesc != null) {
                return externalDesc.typeDesc;
            }
            return getQualifiedName(typeHandler.type);
        }
        private void generateMapper(final Class<?> type) {
            final MethodMapper methodMapper = methodMapper(type);
            tabsln("yass.methodMapper(%s, [", getQualifiedName(type));
            for (final Method method : getMethods(type)) {
                final MethodMapper.Mapping mapping = methodMapper.mapMethod(method);
                tab();
                tabsln("yass.MethodMapping(%s, '%s', %s),", mapping.id, mapping.method.getName(), pyBool(mapping.oneWay));
            }
            tabsln("])");
        }
    }

}
