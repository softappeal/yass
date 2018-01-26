package ch.softappeal.yass.tutorial.shared;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.tutorial.contract.ApplicationException;
import ch.softappeal.yass.tutorial.contract.SystemException;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Method;

public final class UnexpectedExceptionHandler implements Interceptor {

    private UnexpectedExceptionHandler() {
        // disable
    }

    /**
     * Swallows exceptions of oneWay methods (these are logged in {@link Logger}).
     */
    @Override public @Nullable Object invoke(final Method method, final @Nullable Object[] arguments, final Invocation invocation) throws Exception {
        try {
            return invocation.proceed();
        } catch (final Exception e) {
            if (method.isAnnotationPresent(OneWay.class)) {
                return null; // swallow exception
            }
            if (e instanceof ApplicationException) {
                throw e; // pass through contract exception
            }
            throw new SystemException(e.getClass().getName() + ": " + e.getMessage()); // remap unexpected exception to a contract exception
        }
    }

    public static final Interceptor INSTANCE = new UnexpectedExceptionHandler();

}
