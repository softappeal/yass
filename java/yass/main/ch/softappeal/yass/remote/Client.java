package ch.softappeal.yass.remote;

import ch.softappeal.yass.Interceptor;
import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.Reference;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public abstract class Client {

    @FunctionalInterface public interface Tunnel {
        void invoke(Request request) throws Exception;
    }

    public static abstract class Invocation extends AbstractInvocation {
        Invocation(final MethodMapper.Mapping methodMapping, final @Nullable Object[] arguments) {
            super(methodMapping, (arguments == null) ? List.of() : Arrays.asList(arguments));
        }
        public abstract void invoke(boolean asyncSupported, Tunnel tunnel) throws Exception;
        public abstract void settle(Reply reply) throws Exception;
    }

    /**
     * @param invocation {@link Invocation#invoke(boolean, Tunnel)} must be called
     */
    protected abstract void invoke(Invocation invocation) throws Exception;

    protected Object syncInvoke(final ContractId<?> contractId, final Interceptor interceptor, final Method method, final @Nullable Object[] arguments) throws Exception {
        return interceptor.invoke(method, arguments, () -> {
            final var methodMapping = contractId.methodMapper.mapMethod(method);
            final var ready = methodMapping.oneWay ? null : new CountDownLatch(1);
            final var r = Reference.<Reply>create();
            invoke(new Invocation(methodMapping, arguments) {
                @Override public void invoke(final boolean asyncSupported, final Tunnel tunnel) throws Exception {
                    tunnel.invoke(new Request(contractId.id, methodMapping.id, arguments));
                }
                @Override public void settle(final Reply reply) {
                    if (ready == null) {
                        return;
                    }
                    r.set(Objects.requireNonNull(reply));
                    ready.countDown();
                }
            });
            if (ready == null) {
                return null;
            }
            ready.await();
            return r.get().process();
        });
    }

    public final <C> C proxy(final ContractId<C> contractId, final Interceptor... interceptors) {
        final var interceptor = Interceptor.composite(interceptors);
        return Interceptor.proxy(contractId.contract, (proxy, method, arguments) -> syncInvoke(contractId, interceptor, method, arguments));
    }

    public final AsyncProxy async = new AsyncProxy(this);

}
