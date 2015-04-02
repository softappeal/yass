package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public abstract class Client extends Common implements ProxyFactory {

    protected Client(final MethodMapper.Factory methodMapperFactory) {
        super(methodMapperFactory);
    }

    @SuppressWarnings("unchecked")
    @Override public final <C> C proxy(final ContractId<C> contractId, final Interceptor... interceptors) {
        final MethodMapper methodMapper = methodMapper(contractId.contract);
        final Interceptor interceptor = Interceptors.composite(interceptors);
        return (C)Proxy.newProxyInstance(
            contractId.contract.getClassLoader(),
            new Class<?>[] {contractId.contract},
            new InvocationHandler() {
                @Override public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    return Client.this.invoke(new ClientInvocation(interceptor, contractId.id, methodMapper.mapMethod(method), arguments));
                }
            }
        );
    }

    public static final class Invocation {
        public final boolean oneWay;
        private final Interceptor interceptor;
        private final int serviceId;
        private final MethodMapper.Mapping methodMapping;
        @Nullable private final Object[] arguments;
        Invocation(final Interceptor interceptor, final int serviceId, final MethodMapper.Mapping methodMapping, @Nullable final Object[] arguments) {
            oneWay = methodMapping.oneWay;
            this.interceptor = interceptor;
            this.serviceId = serviceId;
            this.methodMapping = methodMapping;
            this.arguments = arguments;
        }
        @Nullable public Object invoke(final Interceptor interceptor, final Tunnel tunnel) throws Throwable {
            return Interceptors.composite(interceptor, this.interceptor).invoke(
                methodMapping.method,
                arguments,
                new Invocation() {
                    @Override public Object proceed() throws Throwable {
                        @Nullable final Reply reply = tunnel.invoke(new Request(serviceId, methodMapping.id, arguments));
                        return oneWay ? null : reply.process();
                    }
                }
            );
        }
    }

    /**
     * @return {@link Invocation#invoke(Interceptor, Tunnel)}
     */
    @Nullable protected abstract Object invoke(Invocation invocation) throws Throwable;

}
