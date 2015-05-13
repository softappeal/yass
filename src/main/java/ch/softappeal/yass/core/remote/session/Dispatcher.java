package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.Server;

/**
 * Each method should execute {@link Runnable#run()} in an own thread.
 */
public interface Dispatcher {

    /**
     * Called if a session has been opened.
     */
    void opened(Runnable runnable) throws Exception;

    /**
     * Called for an incoming request.
     * @param invocation allows dispatching request to different threads
     */
    void invoke(Server.Invocation invocation, Runnable runnable) throws Exception;

}
