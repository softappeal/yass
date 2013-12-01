package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;

public final class SessionSetup {

  final Server server;
  final Executor requestExecutor;
  private final SessionFactory sessionFactory;

  /**
   * @param requestExecutor executes incoming requests
   */
  public SessionSetup(final Server server, final Executor requestExecutor, final SessionFactory sessionFactory) {
    this.server = Check.notNull(server);
    this.requestExecutor = Check.notNull(requestExecutor);
    this.sessionFactory = Check.notNull(sessionFactory);
  }

  public Session createSession(final Connection connection) throws Exception {
    return sessionFactory.create(this, connection);
  }

}
