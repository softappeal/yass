package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class Server extends Common {

    private final class ServiceDesc {
        final Service service;
        final MethodMapper methodMapper;
        ServiceDesc(final Service service) {
            this.service = service;
            methodMapper = methodMapper(service.contractId.contract);
        }
    }

    private final Map<Integer, ServiceDesc> serviceId2serviceDesc;

    public Server(final MethodMapper.Factory methodMapperFactory, final Service... services) {
        super(methodMapperFactory);
        serviceId2serviceDesc = new HashMap<>(services.length);
        for (final Service service : services) {
            if (serviceId2serviceDesc.put(service.contractId.id, new ServiceDesc(service)) != null) {
                throw new IllegalArgumentException("serviceId " + service.contractId.id + " already added");
            }
        }
    }

    public static final class Invocation {
        public final Service service;
        public final Method method;
        public final boolean oneWay;
        public final @Nullable Object[] arguments;
        Invocation(final ServiceDesc serviceDesc, final Request request) {
            service = serviceDesc.service;
            final MethodMapper.Mapping methodMapping = serviceDesc.methodMapper.mapId(request.methodId);
            method = methodMapping.method;
            oneWay = methodMapping.oneWay;
            arguments = request.arguments;
        }
        public Reply invoke() {
            return service.invoke(method, arguments);
        }
    }

    public Invocation invocation(final Request request) {
        final @Nullable ServiceDesc serviceDesc = serviceId2serviceDesc.get(request.serviceId);
        if (serviceDesc == null) {
            throw new RuntimeException("no serviceId " + request.serviceId + " found (methodId " + request.methodId + ')');
        }
        return new Invocation(serviceDesc, request);
    }

}
