package ch.softappeal.yass;

/**
 * Represents a method invocation.
 * @see Interceptor
 */
@FunctionalInterface public interface Invocation {

    /**
     * Proceeds with the invocation.
     * @return the result of the invocation
     * @throws Exception the exception of the invocation
     */
    @Nullable Object proceed() throws Exception;

}
