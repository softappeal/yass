package ch.softappeal.yass.remote;

import ch.softappeal.yass.Nullable;

import java.util.List;

public interface InterceptorAsync<C> {

    /**
     * @return context
     */
    @Nullable C entry(MethodMapper.Mapping methodMapping, List<Object> arguments) throws Exception;

    /**
     * @return result
     */
    @Nullable Object exit(@Nullable C context, @Nullable Object result) throws Exception;

    /**
     * @return exception
     */
    Exception exception(@Nullable C context, Exception exception) throws Exception;

}
