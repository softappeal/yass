package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

/**
 * Invokes a remote method.
 */
@FunctionalInterface public interface Tunnel {

    /**
     * @return will be ignored if it is an oneway invocation, only then can it be null
     */
    @Nullable Reply invoke(Request request) throws Exception;

}
