package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Server {

    private final Map<Integer, Service> id2service;

    public Server(final Service... services) {
        id2service = new HashMap<>(services.length);
        for (final var service : services) {
            if (id2service.put(service.contractId.id, service) != null) {
                throw new IllegalArgumentException("serviceId " + service.contractId.id + " already added");
            }
        }
    }

    private static final ThreadLocal<Completer> COMPLETER = new ThreadLocal<>();

    /**
     * @return completer for active asynchronous service invocation
     * @see ContractId#serviceAsync(Object, InterceptorAsync)
     */
    public static Completer completer() {
        final var completer = COMPLETER.get();
        if (completer == null) {
            throw new IllegalStateException("no active asynchronous request/reply service invocation");
        }
        return completer;
    }

    @FunctionalInterface public interface ReplyWriter {
        void writeReply(Reply reply) throws Exception;
    }

    public static final class Invocation extends AbstractInvocation {
        public final Service service;
        Invocation(final Service service, final Request request) {
            super(
                service.contractId.methodMapper.mapId(request.methodId),
                request.arguments,
                service.async() ? service.interceptorAsync() : null
            );
            this.service = service;
        }
        public void invoke(final ReplyWriter replyWriter) throws Exception {
            if (async()) {
                final var oldCompleter = COMPLETER.get();
                COMPLETER.set(methodMapping.oneWay ? null : new Completer() {
                    @Override public void complete(final @Nullable Object result) {
                        try {
                            replyWriter.writeReply(new ValueReply(exit(result)));
                        } catch (final Exception e) {
                            throw Exceptions.wrap(e);
                        }
                    }
                    @Override public void completeExceptionally(final Exception exception) {
                        Objects.requireNonNull(exception);
                        try {
                            replyWriter.writeReply(new ExceptionReply(exception(exception)));
                        } catch (final Exception e) {
                            throw Exceptions.wrap(e);
                        }
                    }
                });
                try {
                    entry();
                    service.invokeAsync(methodMapping, arguments);
                } finally {
                    COMPLETER.set(oldCompleter);
                }
            } else {
                replyWriter.writeReply(service.invoke(methodMapping, arguments));
            }
        }
    }

    public Invocation invocation(final boolean asyncSupported, final Request request) {
        final var service = id2service.get(request.serviceId);
        if (service == null) {
            throw new RuntimeException("no serviceId " + request.serviceId + " found (methodId " + request.methodId + ')');
        }
        if (!asyncSupported && service.async()) {
            throw new UnsupportedOperationException("asynchronous services not supported (serviceId = " + service.contractId.id + ')');
        }
        return new Invocation(service, request);
    }

    public static final Server EMPTY = new Server();

}
