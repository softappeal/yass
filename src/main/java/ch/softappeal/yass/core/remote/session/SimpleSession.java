package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;

/**
 * Uses an executor for dispatching.
 */
public abstract class SimpleSession extends Session {

    private final Executor dispatchExecutor;

    protected SimpleSession(final Connection connection, final Executor dispatchExecutor) {
        super(connection);
        this.dispatchExecutor = Check.notNull(dispatchExecutor);
    }

    @Override protected final void dispatchOpened(final Runnable runnable) {
        dispatchExecutor.execute(runnable);
    }

    @Override protected final void dispatchServerInvoke(final Server.Invocation invocation, final Runnable runnable) {
        dispatchExecutor.execute(runnable);
    }

}
