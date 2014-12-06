package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class Service {

    final ContractId<?> contractId;
    private final Object implementation;
    private final Interceptor interceptor;

    /**
     * It's a good idea to add an interceptor that handles unexpected exceptions
     * (this is especially useful for oneway methods where these are ignored and NOT passed to the client).
     */
    public <C> Service(final ContractId<C> contractId, final C implementation, final Interceptor... interceptors) {
        this.contractId = Check.notNull(contractId);
        this.implementation = Check.notNull(implementation);
        interceptor = Interceptor.composite(interceptors);
    }

    Reply invoke(final Interceptor interceptor, final Method method, @Nullable final Object[] arguments) {
        try {
            return new ValueReply(Interceptor.composite(interceptor, this.interceptor).invoke(
                method,
                arguments,
                () -> {
                    try {
                        return method.invoke(implementation, arguments);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            ));
        } catch (final Throwable t) {
            return new ExceptionReply(t);
        }
    }

}
