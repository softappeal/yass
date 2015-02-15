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

    public interface Connector {
        /**
         * @throws Exception note: will be ignored
         */
        void connect(SessionFactory sessionFactory) throws Exception;
    }

    /**
     * @param executor must interrupt it's threads to terminate reconnects (use {@link ExecutorService#shutdownNow()})
     */
    public static void start(
        final Executor executor, final long initialDelaySeconds, final long delaySeconds,
        final SessionFactory sessionFactory, final Connector connector
    ) {
        Check.notNull(sessionFactory);
        Check.notNull(connector);
        final AtomicReference<Session> session = new AtomicReference<>(null);
        final SessionFactory factory = new SessionFactory() {
            @Override public Session create(final SessionClient sessionClient) throws Exception {
                final Session s = sessionFactory.create(sessionClient);
                session.set(s);
                return s;
            }
        };
        executor.execute(new Runnable() {
            @Override public void run() {
                if (initialDelaySeconds > 0) {
                    try {
                        TimeUnit.SECONDS.sleep(initialDelaySeconds);
                    } catch (final InterruptedException ignore) {
                        return;
                    }
                }
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
                        TimeUnit.SECONDS.sleep(delaySeconds);
                    } catch (final InterruptedException ignore) {
                        return;
                    }
                }
            }
        });
    }

    /**
     * @see #start(Executor, long, long, SessionFactory, Connector)
     */
    public static void start(
        final Executor executor, final long delaySeconds,
        final SessionFactory sessionFactory, final Connector connector
    ) {
        start(executor, 0, delaySeconds, sessionFactory, connector);
    }

}
