package ch.softappeal.yass.remote.session;

import ch.softappeal.yass.remote.Server;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Uses an executor for dispatching.
 */
public abstract class SimpleSession extends Session {

    protected final Executor dispatchExecutor;

    protected SimpleSession(final Connection connection, final Executor dispatchExecutor) {
        super(connection);
        this.dispatchExecutor = Objects.requireNonNull(dispatchExecutor);
    }

    @Override protected final void dispatchOpened(final Runnable runnable) {
        dispatchExecutor.execute(runnable);
    }

    @Override protected final void dispatchServerInvoke(final Server.Invocation invocation, final Runnable runnable) {
        dispatchExecutor.execute(runnable);
    }

}
