package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationTargetException;
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

    private static final ThreadLocal<Completer> COMPLETER = new ThreadLocal<>();

    /**
     * @return completer for active asynchronous service invocation
     * @see ContractId#serviceAsync(Object, InterceptorAsync)
     */
    public static Completer completer() {
        final @Nullable Completer completer = COMPLETER.get();
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
                final @Nullable Completer oldCompleter = COMPLETER.get();
                COMPLETER.set(methodMapping.oneWay ? null : new Completer() {
                    @Override public void complete(final @Nullable Object result) {
                        try {
                            exit(result);
                            replyWriter.writeReply(new ValueReply(result));
                        } catch (final Exception e) {
                            throw Exceptions.wrap(e);
                        }
                    }
                    @Override public void completeExceptionally(final Exception exception) {
                        try {
                            exception(exception);
                            replyWriter.writeReply(new ExceptionReply(exception));
                        } catch (final Exception e) {
                            throw Exceptions.wrap(e);
                        }
                    }
                });
                try {
                    entry();
                    methodMapping.method.invoke(service.implementation, arguments);
                } catch (final InvocationTargetException e) {
                    try {
                        throw e.getCause();
                    } catch (final Exception | Error e2) {
                        throw e2;
                    } catch (final Throwable t) {
                        throw new Error(t);
                    }
                } finally {
                    COMPLETER.set(oldCompleter);
                }
            } else {
                replyWriter.writeReply(service.invokeSync(methodMapping.method, arguments));
            }
        }
    }

    public Invocation invocation(final boolean asyncSupported, final Request request) {
        final @Nullable Service service = id2service.get(request.serviceId);
        if (service == null) {
            throw new RuntimeException("no serviceId " + request.serviceId + " found (methodId " + request.methodId + ')');
        }
        if (service.async() && !asyncSupported) {
            throw new UnsupportedOperationException("asynchronous services not supported (serviceId = " + service.contractId.id + ')');
        }
        return new Invocation(service, request);
    }

    public static final Server EMPTY = new Server();

}
