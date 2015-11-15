package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

/**
 * Invokes a remote method.
 */
public interface Tunnel {

    /**
     * @return will be ignored if it is an oneWay invocation, only then can it be null
     */
    @Nullable Reply invoke(Request request) throws Exception;

}
