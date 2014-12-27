package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ReconnectSession extends Session {

    private final AtomicBoolean closed;

    protected ReconnectSession(final SessionClient sessionClient, final AtomicBoolean closed) {
        super(sessionClient);
        this.closed = Check.notNull(closed);
    }

    @Override protected final void closed(@Nullable final Throwable throwable) {
        closed.set(true);
        reconnectClosed(throwable);
    }

    /**
     * @see #closed(Throwable)
     */
    protected abstract void reconnectClosed(@Nullable Throwable throwable);

    @FunctionalInterface public interface Factory {
        ReconnectSession create(SessionClient sessionClient, AtomicBoolean closed) throws Exception;
    }

    @FunctionalInterface public interface Connector {
        /**
         * note: must not throw any exceptions
         */
        void connect(SessionFactory sessionFactory);
    }

    /**
     * @param reconnectExecutor must interrupt it's threads to terminate reconnects (use {@link ExecutorService#shutdownNow()})
     */
    public static void start(final Executor reconnectExecutor, final long reconnectIntervalSeconds, final Connector connector, final Factory sessionFactory) {
        Check.notNull(connector);
        Check.notNull(sessionFactory);
        final AtomicBoolean closed = new AtomicBoolean(true);
        final SessionFactory factory = sessionClient -> {
            final ReconnectSession session = sessionFactory.create(sessionClient, closed);
            closed.set(false);
            return session;
        };
        reconnectExecutor.execute(() -> {
            while (!Thread.interrupted()) {
                if (closed.get()) {
                    connector.connect(factory);
                }
                try {
                    TimeUnit.SECONDS.sleep(reconnectIntervalSeconds);
                } catch (final InterruptedException ignore) {
                    return;
                }
            }
        });
    }

}
