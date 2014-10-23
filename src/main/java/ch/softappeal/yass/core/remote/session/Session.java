package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Invoker;
import ch.softappeal.yass.core.remote.InvokerFactory;
import ch.softappeal.yass.util.Nullable;

public abstract class Session implements AutoCloseable, InvokerFactory {

  static final ThreadLocal<Session> INSTANCE = new ThreadLocal<>();

  /**
   * @return the session of the active invocation or null if no active invocation
   */
  @Nullable public static Session get() {
    return INSTANCE.get();
  }

  public final Connection connection;
  private final SessionClient sessionClient;

  protected Session(final SessionClient sessionClient) {
    connection = sessionClient.connection;
    this.sessionClient = sessionClient;
  }

  /**
   * Called from {@link SessionSetup#requestExecutor}.
   * <p>
   * This implementation does nothing.
   * @throws Exception if an exception is thrown, {@link #closed(Throwable)} will be called
   */
  protected void opened() throws Exception {
    // empty
  }

  /**
   * Called when {@link SessionClient} has been closed.
   * @param throwable null if regular close else reason for close
   */
  protected abstract void closed(@Nullable Throwable throwable);

  /**
   * This method is idempotent.
   */
  @Override public void close() {
    sessionClient.close();
  }

  @Override public final <C> Invoker<C> invoker(final ContractId<C> contractId) {
    return sessionClient.invoker(contractId);
  }

}
