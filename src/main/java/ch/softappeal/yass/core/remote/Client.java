package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Proxy;

public abstract class Client extends Common implements InvokerFactory {

    protected Client(final MethodMapper.Factory methodMapperFactory) {
        super(methodMapperFactory);
    }

    @SuppressWarnings("unchecked")
    @Override public final <C> Invoker<C> invoker(final ContractId<C> contractId) {
        final MethodMapper methodMapper = methodMapper(contractId.contract);
        return interceptors -> {
            final Interceptor interceptor = Interceptor.composite(interceptors);
            return (C)Proxy.newProxyInstance(
                contractId.contract.getClassLoader(),
                new Class<?>[] {contractId.contract},
                (proxy, method, arguments) -> invoke(new ClientInvocation(interceptor, contractId.id, methodMapper.mapMethod(method), arguments))
            );
        };
    }

    public static final class ClientInvocation {
        public final boolean oneWay;
        private final Interceptor interceptor;
        private final Object serviceId;
        private final MethodMapper.Mapping methodMapping;
        @Nullable private final Object[] arguments;
        ClientInvocation(final Interceptor interceptor, final Object serviceId, final MethodMapper.Mapping methodMapping, @Nullable final Object[] arguments) {
            oneWay = methodMapping.oneWay;
            this.interceptor = interceptor;
            this.serviceId = serviceId;
            this.methodMapping = methodMapping;
            this.arguments = arguments;
        }
        @Nullable public Object invoke(final Interceptor prependInterceptor, final Tunnel tunnel) throws Throwable {
            return Interceptor.composite(prependInterceptor, interceptor).invoke(
                methodMapping.method,
                arguments,
                () -> {
                    @Nullable final Reply reply = tunnel.invoke(new Request(serviceId, methodMapping.id, arguments));
                    return oneWay ? null : reply.process();
                }
            );
        }
    }

    /**
     * @return {@link ClientInvocation#invoke(Interceptor, Tunnel)}
     */
    @Nullable protected abstract Object invoke(ClientInvocation invocation) throws Throwable;

}
