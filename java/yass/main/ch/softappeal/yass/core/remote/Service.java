package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

public final class Service {

    public final ContractId<?> contractId;
    private final Object implementation;
    private final Object interceptor;

    private <C> Service(final ContractId<C> contractId, final C implementation, final Object interceptor) {
        this.contractId = Objects.requireNonNull(contractId);
        this.implementation = Objects.requireNonNull(implementation);
        this.interceptor = Objects.requireNonNull(interceptor);
    }

    <C> Service(final ContractId<C> contractId, final C implementation, final Interceptor interceptor) {
        this(contractId, implementation, (Object)interceptor);
    }

    <C> Service(final ContractId<C> contractId, final C implementation, final InterceptorAsync<?> interceptor) {
        this(contractId, implementation, (Object)interceptor);
    }

    boolean async() {
        return interceptor instanceof InterceptorAsync;
    }

    private Interceptor interceptor() {
        return (Interceptor)interceptor;
    }

    @SuppressWarnings("unchecked")
    InterceptorAsync<Object> interceptorAsync() {
        return (InterceptorAsync)interceptor;
    }

    Reply invoke(final MethodMapper.Mapping methodMapping, final List<Object> arguments) throws Exception {
        try {
            return new ValueReply(Interceptor.invoke(interceptor(), methodMapping.method, arguments.toArray(), implementation));
        } catch (final Exception e) {
            if (methodMapping.oneWay) {
                throw e;
            }
            return new ExceptionReply(e);
        }
    }

    void invokeAsync(final MethodMapper.Mapping methodMapping, final List<Object> arguments) throws Exception {
        try {
            methodMapping.method.invoke(implementation, arguments.toArray());
        } catch (final InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (final Exception | Error e2) {
                throw e2;
            } catch (final Throwable t) {
                throw new Error(t);
            }
        }
    }

}
