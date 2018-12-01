package ch.softappeal.yass.tutorial.shared;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.tutorial.contract.*;
import kotlin.jvm.functions.*;

import java.lang.reflect.*;
import java.util.*;

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
