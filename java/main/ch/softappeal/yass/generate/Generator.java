package ch.softappeal.yass.generate;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.Services;
import ch.softappeal.yass.serialize.fast.FastSerializer;
import ch.softappeal.yass.serialize.fast.TypeHandler;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public abstract class Generator {

    private static final Set<Type> ROOT_CLASSES = new HashSet<>(Arrays.asList(
        Object.class,
        Exception.class,
        RuntimeException.class,
        Error.class,
        Throwable.class
    ));

    protected static boolean isRootClass(final Class<?> type) {
        return ROOT_CLASSES.contains(Objects.requireNonNull(type));
    }

    public static final class ServiceDesc {
        public final String name;
        public final ContractId<?> contractId;
        private ServiceDesc(final String name, final ContractId<?> contractId) {
            this.name = Objects.requireNonNull(name);
            this.contractId = Objects.requireNonNull(contractId);
        }
    }

    protected static List<ServiceDesc> getServiceDescs(final Services services) throws Exception {
        final List<ServiceDesc> serviceDescs = new ArrayList<>();
        for (final Field field : services.getClass().getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && (field.getType() == ContractId.class)) {
                serviceDescs.add(new ServiceDesc(field.getName(), (ContractId<?>)field.get(services)));
            }
        }
        serviceDescs.sort(Comparator.comparing(sd -> sd.contractId.id));
        return serviceDescs;
    }

    private static Set<Class<?>> getInterfaces(final @Nullable Services services) throws Exception {
        if (services == null) {
            return new HashSet<>();
        }
        return getServiceDescs(services).stream().map(serviceDesc -> serviceDesc.contractId.contract).collect(Collectors.toSet());
    }

    protected static Method[] getMethods(final Class<?> type) {
        final Method[] methods = type.getMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        return methods;
    }

    private final String rootPackage;
    protected final SortedMap<Integer, TypeHandler> id2typeHandler;
    protected final @Nullable Services initiator;
    protected final @Nullable Services acceptor;
    private MethodMapper.@Nullable Factory methodMapperFactory;
    protected final SortedSet<Class<?>> interfaces = new TreeSet<>(Comparator.comparing(Class::getCanonicalName));

    protected final void checkType(final Class<?> type) {
        if (!type.getCanonicalName().startsWith(rootPackage)) {
            throw new RuntimeException("type '" + type.getCanonicalName() + "' doesn't have root package '" + rootPackage + "'");
        }
    }

    protected final String qualifiedName(final Class<?> type) {
        return type.getCanonicalName().substring(rootPackage.length());
    }

    protected final MethodMapper methodMapper(final Class<?> type) {
        return methodMapperFactory.create(type);
    }

    protected Generator(final String rootPackage, final FastSerializer serializer, final @Nullable Services initiator, final @Nullable Services acceptor) throws Exception {
        this.rootPackage = rootPackage.isEmpty() ? "" : rootPackage + '.';
        id2typeHandler = serializer.id2typeHandler();
        this.initiator = initiator;
        this.acceptor = acceptor;
        if ((initiator != null) && (acceptor != null) && (initiator.methodMapperFactory != acceptor.methodMapperFactory)) {
            throw new IllegalArgumentException("initiator and acceptor must have same methodMapperFactory");
        }
        if (initiator != null) {
            methodMapperFactory = initiator.methodMapperFactory;
        }
        if (acceptor != null) {
            methodMapperFactory = acceptor.methodMapperFactory;
        }
        interfaces.addAll(getInterfaces(initiator));
        interfaces.addAll(getInterfaces(acceptor));
        interfaces.forEach(this::checkType);
    }

}
