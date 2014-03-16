package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Invoker;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;

public abstract class SessionSetup {

  public final Server server;
  public final Executor requestExecutor;

  /**
   * @param requestExecutor executes incoming requests
   */
  protected SessionSetup(final Server server, final Executor requestExecutor) {
    this.server = Check.notNull(server);
    this.requestExecutor = Check.notNull(requestExecutor);
  }

  /**
   * Creates a new session.
   * It's allowed to call {@link Invoker#proxy(Interceptor...)} during this method,
   * but the proxies can be used not before {@link Session#opened()} is called.
   * If this method throws an exception, the connection is rejected ({@link Session#closed(Throwable)} won't be called).
   */
  public abstract Session createSession(SessionClient sessionClient) throws Exception;

}
