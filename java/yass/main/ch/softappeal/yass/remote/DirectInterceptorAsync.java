package ch.softappeal.yass.remote;

import ch.softappeal.yass.Nullable;

import java.util.List;

public class DirectInterceptorAsync<C> implements InterceptorAsync<C> {

    @Override public C entry(final MethodMapper.Mapping methodMapping, final List<Object> arguments) {
        return null;
    }

    @Override public Object exit(final @Nullable C context, final @Nullable Object result) {
        return result;
    }

    @Override public Exception exception(final @Nullable C context, final Exception exception) {
        return exception;
    }

    public static final InterceptorAsync<Object> INSTANCE = new DirectInterceptorAsync<>();

}
