package ch.softappeal.yass.tutorial.shared;

import ch.softappeal.yass.remote.OneWay;
import ch.softappeal.yass.tutorial.contract.ApplicationException;
import ch.softappeal.yass.tutorial.contract.SystemException;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;

import java.lang.reflect.Method;
import java.util.List;

public final class UnexpectedExceptionHandler implements Function3<Method, List<?>, Function0<?>, Object> {

    private UnexpectedExceptionHandler() {
        // disable
    }

    /**
     * Swallows exceptions of oneWay methods (these are logged in {@link Logger}).
     */
    @Override
    public Object invoke(final Method method, final List<?> arguments, final Function0<?> invocation) {
        try {
            return invocation.invoke();
        } catch (final Exception e) {
            if (method.isAnnotationPresent(OneWay.class)) {
                return null; // swallow exception
            }
            if (e instanceof ApplicationException) {
                throw e; // pass through contract exception
            }
            // remap unexpected exception to a contract exception
            throw new SystemException(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public static final Function3<Method, List<?>, Function0<?>, Object> INSTANCE = new UnexpectedExceptionHandler();

}
