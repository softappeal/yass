package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
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

    public static final class ServerInvocation {
        public final boolean oneWay;
        private final Service service;
        private final Method method;
        @Nullable private final Object[] arguments;
        ServerInvocation(final ServiceDesc serviceDesc, final Request request) {
            final MethodMapper.Mapping methodMapping = serviceDesc.methodMapper.mapId(request.methodId);
            oneWay = methodMapping.oneWay;
            service = serviceDesc.service;
            method = methodMapping.method;
            arguments = request.arguments;
        }
        public Reply invoke(final Interceptor interceptor) {
            return service.invoke(interceptor, method, arguments);
        }
    }

    public ServerInvocation invocation(final Request request) {
        @Nullable final ServiceDesc serviceDesc = serviceId2serviceDesc.get(request.serviceId);
        if (serviceDesc == null) {
            throw new RuntimeException("no serviceId " + request.serviceId + " found (methodId " + request.methodId + ')');
        }
        return new ServerInvocation(serviceDesc, request);
    }

}
