package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.ProxyFactory;
import ch.softappeal.yass.util.Nullable;

public abstract class Session implements AutoCloseable, ProxyFactory {

    static final ThreadLocal<Session> INSTANCE = new ThreadLocal<>();

    /**
     * @return the session of the active invocation or null if no active invocation
     */
    public static @Nullable Session get() {
        return INSTANCE.get();
    }

    public final Connection connection;
    private final SessionClient sessionClient;

    protected Session(final SessionClient sessionClient) {
        connection = sessionClient.connection;
        this.sessionClient = sessionClient;
    }

    /**
     * Called when the session has been opened.
     * This implementation does nothing.
     * @throws Exception if an exception is thrown, {@link #closed(Throwable)} will be called
     */
    protected void opened() throws Exception {
        // empty
    }

    /**
     * Called when the session has been closed.
     * @param throwable null if regular close else reason for close
     */
    protected abstract void closed(@Nullable Throwable throwable) throws Exception;

    /**
     * Closes the session.
     * This method is idempotent.
     */
    @Override public void close() {
        sessionClient.close();
    }

    @Override public final <C> C proxy(final ContractId<C> contractId, final Interceptor... interceptors) {
        return sessionClient.proxy(contractId, interceptors);
    }

    public final boolean isClosed() {
        return sessionClient.closed.get();
    }

}
