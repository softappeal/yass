package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.Server;

/**
 * Each method must call {@link Runnable#run()} (possibly in an own thread).
 * The additional method parameters could be used by the dispatching algorithm.
 */
public interface Dispatcher {

    /**
     * Called if a session has been opened.
     */
    void opened(Session session, Runnable runnable) throws Exception;

    /**
     * Called for an incoming request.
     */
    void invoke(Session session, Server.Invocation invocation, Runnable runnable) throws Exception;

}
