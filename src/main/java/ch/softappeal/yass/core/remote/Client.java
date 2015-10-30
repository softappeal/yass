package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Proxy;

public abstract class Client extends Common implements ProxyFactory {

    protected Client(final MethodMapper.Factory methodMapperFactory) {
        super(methodMapperFactory);
    }

    @SuppressWarnings("unchecked")
    @Override public final <C> C proxy(final ContractId<C> contractId, final Interceptor... interceptors) {
        final MethodMapper methodMapper = methodMapper(contractId.contract);
        final Interceptor interceptor = Interceptor.composite(interceptors);
        return (C)Proxy.newProxyInstance(
            contractId.contract.getClassLoader(),
            new Class<?>[] {contractId.contract},
            (proxy, method, arguments) -> invoke(new Invocation(interceptor, contractId.id, methodMapper.mapMethod(method), arguments))
        );
    }

    public static final class Invocation {
        private final Interceptor interceptor;
        private final int serviceId;
        private final MethodMapper.Mapping methodMapping;
        public final boolean oneWay;
        @Nullable private final Object[] arguments;
        Invocation(final Interceptor interceptor, final int serviceId, final MethodMapper.Mapping methodMapping, @Nullable final Object[] arguments) {
            this.interceptor = interceptor;
            this.serviceId = serviceId;
            this.methodMapping = methodMapping;
            oneWay = methodMapping.oneWay;
            this.arguments = arguments;
        }
        @Nullable public Object invoke(final Interceptor interceptor, final Tunnel tunnel) throws Throwable {
            return Interceptor.composite(interceptor, this.interceptor).invoke(
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
     * @return {@link Invocation#invoke(Interceptor, Tunnel)}
     */
    @Nullable protected abstract Object invoke(Invocation invocation) throws Throwable;

}
