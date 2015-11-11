package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Method;

public final class UnexpectedExceptionHandler implements Interceptor {

    private UnexpectedExceptionHandler() {
        // disable
    }

    @Override public Object invoke(final Method method, final @Nullable Object[] arguments, final Invocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } catch (final ApplicationException e) { // pass through contract exception
            throw e;
        } catch (final Exception e) { // remap unexpected exception to a contract exception
            throw new SystemException(e.getClass().getName() + ": " + e.getMessage());
        } catch (final Throwable t) { // terminate on Throwable
            Exceptions.uncaughtException(Exceptions.TERMINATE, t);
            return null;
        }
    }

    public static final Interceptor INSTANCE = new UnexpectedExceptionHandler();

}
