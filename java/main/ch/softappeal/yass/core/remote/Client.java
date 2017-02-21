package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public abstract class Client {

    @FunctionalInterface public interface Tunnel {
        void invoke(Request request) throws Exception;
    }

    public static final class Invocation extends AbstractInvocation {
        private final @Nullable CompletableFuture<?> promise;
        private final int serviceId;
        Invocation(
            final MethodMapper.Mapping methodMapping, final @Nullable Object[] arguments, final @Nullable InterceptorAsync<Object> interceptor,
            final @Nullable CompletableFuture<?> promise, final int serviceId
        ) {
            super(methodMapping, (arguments == null) ? Collections.emptyList() : Arrays.asList(arguments), interceptor);
            this.promise = promise;
            this.serviceId = serviceId;
        }
        public void invoke(final boolean asyncSupported, final Tunnel tunnel) throws Exception {
            if (async()) {
                if (!asyncSupported) {
                    throw new UnsupportedOperationException("asynchronous services not supported (serviceId = " + serviceId + ')');
                }
                entry();
            }
            tunnel.invoke(new Request(serviceId, methodMapping.id, arguments));
        }
        @SuppressWarnings("unchecked")
        public void settle(final Reply reply) throws Exception {
            if (promise == null) {
                return; // oneWay
            }
            if (async()) {
                try {
                    exit(reply.process());
                } catch (final Exception e) {
                    exception(e);
                }
            }
            try {
                ((CompletableFuture)promise).complete(reply.process());
            } catch (final Exception e) {
                promise.completeExceptionally(e);
            }
        }
    }

    /**
     * @param invocation {@link Client.Invocation#invoke(boolean, Client.Tunnel)} must be called
     */
    protected abstract void invoke(Invocation invocation) throws Exception;

    protected Object invokeSync(final ContractId<?> contractId, final Interceptor interceptor, final Method method, final @Nullable Object[] arguments) throws Exception {
        return interceptor.invoke(method, arguments, () -> {
            final MethodMapper.Mapping methodMapping = contractId.methodMapper.mapMethod(method);
            final @Nullable CompletableFuture<?> promise = methodMapping.oneWay ? null : new CompletableFuture<>();
            invoke(new Invocation(methodMapping, arguments, null, promise, contractId.id));
            if (promise == null) {
                return null; // oneWay
            }
            try {
                return promise.get();
            } catch (final ExecutionException e) {
                try {
                    throw e.getCause();
                } catch (final Exception | Error e2) {
                    throw e2;
                } catch (final Throwable t) {
                    throw new Error(t);
                }
            }
        });
    }

    public final <C> C proxy(final ContractId<C> contractId, final Interceptor... interceptors) {
        final Interceptor interceptor = Interceptor.composite(interceptors);
        return contractId.contract.cast(Proxy.newProxyInstance(
            contractId.contract.getClassLoader(),
            new Class<?>[] {contractId.contract},
            (proxy, method, arguments) -> invokeSync(contractId, interceptor, method, arguments)
        ));
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

    private static final ThreadLocal<CompletableFuture<Object>> PROMISE = new ThreadLocal<>();

    /**
     * @see #promise(Execute)
     * @see #proxyAsync(ContractId)
     */
    @SuppressWarnings("unchecked")
    public final <C> C proxyAsync(final ContractId<C> contractId, final InterceptorAsync<?> interceptor) {
        Check.notNull(interceptor);
        return contractId.contract.cast(Proxy.newProxyInstance(
            contractId.contract.getClassLoader(),
            new Class<?>[] {contractId.contract},
            (proxy, method, arguments) -> {
                final MethodMapper.Mapping methodMapping = contractId.methodMapper.mapMethod(method);
                final @Nullable CompletableFuture<Object> promise = PROMISE.get();
                if (promise == null) {
                    if (!methodMapping.oneWay) {
                        throw new IllegalStateException("asynchronous request/reply proxy call must be enclosed with 'promise' method call");
                    }
                } else if (methodMapping.oneWay) {
                    throw new IllegalStateException("asynchronous oneWay proxy call must not be enclosed with 'promise' method");
                }
                invoke(new Invocation(methodMapping, arguments, (InterceptorAsync)interceptor, promise, contractId.id));
                return handlePrimitiveTypes(method.getReturnType());
            }
        ));
    }

    /**
     * @see #proxyAsync(ContractId, InterceptorAsync)
     */
    public final <C> C proxyAsync(final ContractId<C> contractId) {
        return proxyAsync(contractId, InterceptorAsync.EMPTY);
    }

    @FunctionalInterface public interface Execute<R> {
        R execute() throws Exception;
    }

    /**
     * Gets a promise for an asynchronous service invocation.
     * The usage pattern is:
     * <pre>
     * EchoService echoService = client.proxyAsync(ECHO_SERVICE_ID);
     * Client.promise(() -&gt; echoService.echo("hello")).thenAccept(r -&gt; System.out.println("result: " + r));
     * </pre>
     * @see #proxyAsync(ContractId)
     * @see #promise(VoidExecute)
     */
    @SuppressWarnings("unchecked")
    public static <T> CompletionStage<T> promise(final Execute<T> execute) {
        final @Nullable CompletableFuture<Object> oldPromise = PROMISE.get();
        final CompletableFuture<Object> promise = new CompletableFuture<>();
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

    /**
     * @see #promise(Execute)
     */
    public static CompletionStage<Void> promise(final VoidExecute execute) {
        Check.notNull(execute);
        return promise(() -> {
            execute.execute();
            return null;
        });
    }

}
