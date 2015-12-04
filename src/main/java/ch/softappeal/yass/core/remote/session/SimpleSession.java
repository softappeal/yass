package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;

/**
 * Uses an executor for dispatching.
 */
public abstract class SimpleSession extends Session {

    public final Executor dispatchExecutor;

    protected SimpleSession(final MethodMapper.Factory methodMapperFactory, final Connection connection, final Executor dispatchExecutor) {
        super(methodMapperFactory, connection);
        this.dispatchExecutor = Check.notNull(dispatchExecutor);
    }

    @Override protected final void dispatchOpened(final Runnable runnable) {
        dispatchExecutor.execute(runnable);
    }

    @Override protected final void dispatchServerInvoke(final Server.Invocation invocation, final Runnable runnable) {
        dispatchExecutor.execute(runnable);
    }

}
