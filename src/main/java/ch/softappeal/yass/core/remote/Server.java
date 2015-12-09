package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class Server {

    private final Map<Integer, Service> id2service;

    public Server(final Service... services) {
        id2service = new HashMap<>(services.length);
        for (final Service service : services) {
            if (id2service.put(service.contractId.id, service) != null) {
                throw new IllegalArgumentException("serviceId " + service.contractId.id + " already added");
            }
        }
    }

    public static final class Invocation {
        public final Service service;
        public final MethodMapper.Mapping methodMapping;
        public final @Nullable Object[] arguments;
        Invocation(final Service service, final Request request) {
            this.service = service;
            methodMapping = service.contractId.methodMapper.mapId(request.methodId);
            arguments = request.arguments;
        }
        public Reply invoke() {
            return service.invoke(methodMapping.method, arguments);
        }
    }

    public Invocation invocation(final Request request) {
        final @Nullable Service service = id2service.get(request.serviceId);
        if (service == null) {
            throw new RuntimeException("no serviceId " + request.serviceId + " found (methodId " + request.methodId + ')');
        }
        return new Invocation(service, request);
    }

    public static final Server EMPTY = new Server();

}
