package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

public abstract class Session {

  static final ThreadLocal<Session> INSTANCE = new ThreadLocal<Session>();

  /**
   * @return the session of the active invocation or null if no active invocation
   */
  @Nullable public static Session get() {
    return INSTANCE.get();
  }

  public final SessionClient sessionClient;

  protected Session(final SessionClient sessionClient) {
    this.sessionClient = Check.notNull(sessionClient);
  }

  /**
   * Called from {@link SessionSetup#requestExecutor}.
   * <p/>
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

}
