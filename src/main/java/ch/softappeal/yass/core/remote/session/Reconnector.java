package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class Reconnector {

    private Reconnector() {
        // disable
    }

    @FunctionalInterface public interface Connector {
        /**
         * @throws Exception note: will be ignored
         */
        void connect(SessionFactory sessionFactory) throws Exception;
    }

    /**
     * @param executor must interrupt it's threads to terminate reconnects (use {@link ExecutorService#shutdownNow()})
     */
    public static void start(final Executor executor, final long intervalSeconds, final SessionFactory sessionFactory, final Connector connector) {
        Check.notNull(sessionFactory);
        Check.notNull(connector);
        final AtomicReference<Session> session = new AtomicReference<>(null);
        final SessionFactory factory = sessionClient -> {
            final Session s = sessionFactory.create(sessionClient);
            session.set(s);
            return s;
        };
        executor.execute(() -> {
            while (!Thread.interrupted()) {
                final Session s = session.get();
                if ((s == null) || s.isClosed()) {
                    try {
                        connector.connect(factory);
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

}
