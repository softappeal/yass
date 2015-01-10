package ch.softappeal.yass.core;

import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Intercepts a method invocation.
 */
public interface Interceptor {

    /**
     * Performs extra treatments before and after the invocation.
     * @param arguments see {@link InvocationHandler#invoke(Object, Method, Object[])}
     * @return {@link Invocation#proceed()}
     * @throws Throwable exception of {@link Invocation#proceed()}
     */
    @Nullable Object invoke(Method method, @Nullable Object[] arguments, Invocation invocation) throws Throwable;

}
