package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class Service {

    public final ContractId<?> contractId;
    private final Object implementation;
    private final Interceptor interceptor;

    <C> Service(final ContractId<C> contractId, final C implementation, final Interceptor... interceptors) {
        this.contractId = Check.notNull(contractId);
        this.implementation = Check.notNull(implementation);
        interceptor = Interceptor.composite(interceptors);
    }

    Reply invoke(final Method method, final @Nullable Object[] arguments) {
        try {
            return new ValueReply(interceptor.invoke(method, arguments, () -> {
                try {
                    return method.invoke(implementation, arguments);
                } catch (final InvocationTargetException e) {
                    throw e.getCause();
                }
            }));
        } catch (final Throwable t) { // $$$
            return new ExceptionReply(t);
        }
    }

}
