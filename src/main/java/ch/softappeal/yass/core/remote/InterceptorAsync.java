package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

public interface InterceptorAsync<C> {

    /**
     * @return context
     */
    @Nullable C entry(MethodMapper.Mapping methodMapping, @Nullable Object[] arguments) throws Exception;

    void exit(@Nullable C context, @Nullable Object result) throws Exception;

    void exception(@Nullable C context, Exception exception) throws Exception;

    InterceptorAsync<?> EMPTY = new InterceptorAsync<Object>() {
        @Override public @Nullable Object entry(final MethodMapper.Mapping methodMapping, final @Nullable Object[] arguments) {
            return null;
        }
        @Override public void exit(final @Nullable Object context, final @Nullable Object result) {
            // empty
        }
        @Override public void exception(final @Nullable Object context, final Exception exception) {
            // empty
        }
    };

}
