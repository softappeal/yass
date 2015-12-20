package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Reconnector<S extends Session> {

    private volatile @Nullable S session = null;

    private static boolean connected(final Session session) {
        return (session != null) && !session.isClosed();
    }

    public final boolean connected() {
        return connected(session);
    }

    /**
     * @return current {@link Session}
     * @throws SessionClosedException if no active session
     */
    public final S session() throws SessionClosedException {
        final S session = this.session;
        if (!connected(session)) {
            throw new SessionClosedException();
        }
        return session;
    }

    @FunctionalInterface public interface SessionProxyGetter<S, C> {
        C get(S session) throws Exception;
    }

    /**
     * @return a proxy surviving reconnect
     */
    @SuppressWarnings("unchecked")
    protected final <C> C proxy(final Class<C> contract, final SessionProxyGetter<S, C> sessionProxyGetter) {
        Check.notNull(sessionProxyGetter);
        return (C)Proxy.newProxyInstance(
            contract.getClassLoader(),
            new Class<?>[] {contract},
            (proxy, method, arguments) -> {
                try {
                    return method.invoke(sessionProxyGetter.get(session()), arguments);
                } catch (final InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        );
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
    public final void start(
        final Executor executor, final long initialDelaySeconds, final long delaySeconds,
        final SessionFactory sessionFactory, final Connector connector
    ) {
        Check.notNull(sessionFactory);
        Check.notNull(connector);
        @SuppressWarnings("unchecked") final SessionFactory reconnectorSessionFactory = connection -> {
            final Session session = sessionFactory.create(connection);
            this.session = (S)session;
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
                    session = null;
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
