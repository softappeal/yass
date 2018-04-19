package ch.softappeal.yass.remote;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class Server {

    private final Map<Integer, AbstractService> id2service;

    public Server(final AbstractService... services) {
        id2service = new HashMap<>(services.length);
        for (final var service : services) {
            if (id2service.put(service.contractId.id, service) != null) {
                throw new IllegalArgumentException("serviceId " + service.contractId.id + " already added");
            }
        }
    }

    public static final class Invocation extends AbstractInvocation {
        public final AbstractService service;
        Invocation(final AbstractService service, final Request request) {
            super(service.contractId.methodMapper.mapId(request.methodId), request.arguments);
            this.service = service;
        }
        public void invoke(final AbstractService.ReplyWriter replyWriter) throws Exception {
            service.invoke(this, replyWriter);
        }
    }

    public Invocation invocation(final boolean asyncSupported, final Request request) {
        final var service = Optional.ofNullable(id2service.get(request.serviceId))
            .orElseThrow(() -> new RuntimeException("no serviceId " + request.serviceId + " found (methodId " + request.methodId + ')'));
        if (!asyncSupported && (service instanceof AsyncService)) {
            throw new UnsupportedOperationException("asynchronous services not supported (serviceId = " + service.contractId.id + ')');
        }
        return new Invocation(service, request);
    }

    public static final Server EMPTY = new Server();

}
