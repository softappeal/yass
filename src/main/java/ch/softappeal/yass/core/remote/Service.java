package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Method;

public final class Service {

    public final ContractId<?> contractId;
    final Object implementation;
    private final Object interceptor;

    private <C> Service(final ContractId<C> contractId, final C implementation, final Object interceptor) {
        this.contractId = Check.notNull(contractId);
        this.implementation = Check.notNull(implementation);
        this.interceptor = Check.notNull(interceptor);
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

    Reply invokeSync(final Method method, final @Nullable Object[] arguments) {
        try {
            return new ValueReply(Interceptor.invoke(interceptor(), method, arguments, implementation));
        } catch (final Exception e) {
            return new ExceptionReply(e);
        }
    }

}
