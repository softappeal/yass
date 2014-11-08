package ch.softappeal.yass.core;

import ch.softappeal.yass.util.Nullable;

/**
 * Represents a method invocation.
 * @see Interceptor
 */
@FunctionalInterface public interface Invocation {

    /**
     * Proceeds with the invocation.
     * @return the result of the invocation
     * @throws Throwable the exception of the invocation
     */
    @Nullable Object proceed() throws Throwable;

}
