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
     * @param executor used once; must interrupt it's threads to terminate reconnects (use {@link ExecutorService#shutdownNow()})
     */
    public final void start(
        final Executor executor, final long delaySeconds, final long intervalSeconds,
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
            try {
                TimeUnit.SECONDS.sleep(delaySeconds);
            } catch (final InterruptedException ignore) {
                return;
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
                    TimeUnit.SECONDS.sleep(intervalSeconds);
                } catch (final InterruptedException ignore) {
                    return;
                }
            }
        });
    }

    public final void start(
        final Executor executor, final long intervalSeconds,
        final SessionFactory sessionFactory, final Connector connector
    ) {
        start(executor, 0L, intervalSeconds, sessionFactory, connector);
    }

}
