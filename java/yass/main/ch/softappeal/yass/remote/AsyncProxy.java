package ch.softappeal.yass.remote;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.Interceptor;
import ch.softappeal.yass.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class AsyncProxy {

    private final Client client;

    AsyncProxy(final Client client) {
        this.client = client;
    }

    private static final ThreadLocal<CompletableFuture<Object>> PROMISE = new ThreadLocal<>();

    public <C> C proxy(final ContractId<C> contractId, final AsyncInterceptor interceptor) {
        Objects.requireNonNull(interceptor);
        return Interceptor.proxy(
            contractId.contract,
            (proxy, method, arguments) -> {
                final var methodMapping = contractId.methodMapper.mapMethod(method);
                final var promise = PROMISE.get();
                if (promise == null) {
                    if (!methodMapping.oneWay) {
                        throw new IllegalStateException("asynchronous request/reply proxy call must be enclosed with 'promise' method call");
                    }
                } else if (methodMapping.oneWay) {
                    throw new IllegalStateException("asynchronous oneWay proxy call must not be enclosed with 'promise' method");
                }
                client.invoke(new Client.Invocation(methodMapping, arguments) {
                    @Override public void invoke(final boolean asyncSupported, final Client.Tunnel tunnel) throws Exception {
                        if (!asyncSupported) {
                            throw new UnsupportedOperationException("asynchronous services not supported (serviceId = " + contractId.id + ')');
                        }
                        interceptor.entry(this);
                        tunnel.invoke(new Request(contractId.id, methodMapping.id, arguments));
                    }
                    public void settle(final Reply reply) throws Exception {
                        if (promise == null) {
                            return; // oneWay
                        }
                        try {
                            final @Nullable Object result = reply.process();
                            interceptor.exit(this, result);
                            promise.complete(result);
                        } catch (final Exception e) {
                            interceptor.exception(this, e);
                            promise.completeExceptionally(e);
                        }
                    }
                });
                return handlePrimitiveTypes(method.getReturnType());
            }
        );
    }

    public <C> C proxy(final ContractId<C> contractId) {
        return proxy(contractId, DirectAsyncInterceptor.INSTANCE);
    }

    @FunctionalInterface public interface Execute<R> {
        R execute() throws Exception;
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletionStage<T> promise(final Execute<T> execute) {
        final var oldPromise = PROMISE.get();
        final var promise = new CompletableFuture<>();
        PROMISE.set(promise);
        try {
            execute.execute();
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        } finally {
            PROMISE.set(oldPromise);
        }
        return (CompletionStage)promise;
    }

    @FunctionalInterface public interface VoidExecute {
        void execute() throws Exception;
    }

    public static CompletionStage<Void> promise(final VoidExecute execute) {
        Objects.requireNonNull(execute);
        return promise(() -> {
            execute.execute();
            return null;
        });
    }

    private static @Nullable Object handlePrimitiveTypes(final Class<?> type) {
        if (type == boolean.class) {
            return Boolean.FALSE;
        } else if (type == byte.class) {
            return (byte)0;
        } else if (type == short.class) {
            return (short)0;
        } else if (type == int.class) {
            return 0;
        } else if (type == long.class) {
            return (long)0;
        } else if (type == char.class) {
            return (char)0;
        } else if (type == float.class) {
            return (float)0;
        } else if (type == double.class) {
            return (double)0;
        }
        return null;
    }

}
