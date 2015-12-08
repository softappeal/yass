package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class Reconnector<S extends Session> {

    @FunctionalInterface public interface Connector {
        /**
         * @throws Exception note: will be ignored
         */
        void connect(SessionFactory sessionFactory) throws Exception;
    }

    /**
     * @param executor must interrupt it's threads to terminate reconnects (use {@link ExecutorService#shutdownNow()})
     */
    public Reconnector(
        final Executor executor, final long initialDelaySeconds, final long delaySeconds,
        final SessionFactory sessionFactory, final Connector connector
    ) {
        Check.notNull(sessionFactory);
        Check.notNull(connector);
        final SessionFactory proxySessionFactory = connection -> {
            final Session session = sessionFactory.create(connection);
            this.session = session;
            return session;
        };
        executor.execute(() -> {
            if (initialDelaySeconds > 0) {
                try {
                    TimeUnit.SECONDS.sleep(initialDelaySeconds);
                } catch (final InterruptedException ignore) {
                    return;
                }
            }
            while (!Thread.interrupted()) {
                final Session session = this.session;
                if ((session == null) || session.isClosed()) {
                    this.session = null;
                    try {
                        connector.connect(proxySessionFactory);
                    } catch (final Exception ignore) {
                        // empty
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(delaySeconds);
                } catch (final InterruptedException ignore) {
                    return;
                }
            }
        });
    }

    /**
     * @see #Reconnector(Executor, long, long, SessionFactory, Connector)
     */
    public Reconnector(
        final Executor executor, final long delaySeconds,
        final SessionFactory sessionFactory, final Connector connector
    ) {
        this(executor, 0, delaySeconds, sessionFactory, connector);
    }

    private volatile @Nullable Session session = null;

    /**
     * @return current {@link Session}
     * @throws SessionClosedException if no active session
     */
    @SuppressWarnings("unchecked")
    public S session() throws SessionClosedException {
        final Session session = this.session;
        if (session == null) {
            throw new SessionClosedException();
        }
        return (S)session;
    }

    public boolean connected() {
        return (session != null);
    }

}
