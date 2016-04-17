package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Proxy;

public abstract class Client {

    public final <C> C proxy(final ContractId<C> contractId, final Interceptor... interceptors) {
        final Interceptor interceptor = Interceptor.composite(interceptors);
        return contractId.contract.cast(Proxy.newProxyInstance(
            contractId.contract.getClassLoader(),
            new Class<?>[] {contractId.contract},
            (proxy, method, arguments) -> invoke(new Invocation(interceptor, contractId.id, contractId.methodMapper.mapMethod(method), arguments))
        ));
    }

    @FunctionalInterface public interface Tunnel {
        /**
         * @return will be ignored if it is an oneWay invocation, only then can it be null
         */
        @Nullable Reply invoke(Request request) throws Exception;
    }

    public static final class Invocation {
        private final Interceptor interceptor;
        private final int serviceId;
        public final MethodMapper.Mapping methodMapping;
        private final @Nullable Object[] arguments;
        Invocation(final Interceptor interceptor, final int serviceId, final MethodMapper.Mapping methodMapping, final @Nullable Object[] arguments) {
            this.interceptor = interceptor;
            this.serviceId = serviceId;
            this.methodMapping = methodMapping;
            this.arguments = arguments;
        }
        public @Nullable Object invoke(final Tunnel tunnel) throws Exception {
            return interceptor.invoke(
                methodMapping.method,
                arguments,
                () -> {
                    final @Nullable Reply reply = tunnel.invoke(new Request(serviceId, methodMapping.id, arguments));
                    return methodMapping.oneWay ? null : reply.process();
                }
            );
        }
    }

    /**
     * @return {@link Invocation#invoke(Client.Tunnel)}
     */
    protected abstract @Nullable Object invoke(Invocation invocation) throws Exception;

}
