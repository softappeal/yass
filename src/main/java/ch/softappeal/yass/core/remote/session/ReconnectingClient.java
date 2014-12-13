package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ReconnectingClient {

    private volatile Session session = null;

    public final boolean isConnected() {
        return (session != null);
    }

    /**
     * @param reconnectExecutor must interrupt it's threads to terminate the reconnections (use {@link ExecutorService#shutdownNow()})
     */
    protected ReconnectingClient(final Executor reconnectExecutor, final long reconnectIntervalSeconds) {
        final SessionFactory sessionFactory = sessionClient -> new Session(sessionClient) {
            @Override protected void opened() throws Exception {
                session = this;
                connected(session);
            }
            @Override protected void closed(@Nullable final Throwable throwable) {
                session = null;
                disconnected(throwable);
            }
        };
        reconnectExecutor.execute(() -> {
            while (!Thread.interrupted()) {
                if (!isConnected()) {
                    try {
                        connect(sessionFactory);
                    } catch (final Exception e) {
                        connectFailed(e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(reconnectIntervalSeconds);
                } catch (final InterruptedException ignore) {
                    return;
                }

            }
        });
    }

    protected abstract void connect(SessionFactory sessionFactory) throws Exception;

    protected void connectFailed(final Exception e) {
        // empty
    }

    protected abstract void connected(Session session) throws Exception;

    /**
     * @param throwable see {@link Session#closed(Throwable)}
     */
    protected abstract void disconnected(@Nullable Throwable throwable);

}
