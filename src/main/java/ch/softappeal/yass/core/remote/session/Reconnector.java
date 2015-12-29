package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides proxies surviving reconnects.
 */
public class Reconnector<S extends Session> extends ProxyDelegate<S> {

    @FunctionalInterface public interface Connector {
        /**
         * @throws Exception note: will be ignored
         */
        void connect(SessionFactory sessionFactory) throws Exception;
    }

    /**
     * @param executor must interrupt it's threads to terminate reconnects (use {@link ExecutorService#shutdownNow()})
     */
    public final void start(
        final Executor executor, final long initialDelaySeconds, final long delaySeconds,
        final SessionFactory sessionFactory, final Connector connector
    ) {
        Check.notNull(sessionFactory);
        Check.notNull(connector);
        @SuppressWarnings("unchecked") final SessionFactory reconnectorSessionFactory = connection -> {
            final Session session = sessionFactory.create(connection);
            session((S)session);
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
                if (!connected()) {
                    session(null);
                    try {
                        connector.connect(reconnectorSessionFactory);
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
     * @see #start(Executor, long, long, SessionFactory, Connector)
     */
    public final void start(
        final Executor executor, final long delaySeconds,
        final SessionFactory sessionFactory, final Connector connector
    ) {
        start(executor, 0, delaySeconds, sessionFactory, connector);
    }

}
