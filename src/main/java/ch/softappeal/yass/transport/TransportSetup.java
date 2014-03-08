package ch.softappeal.yass.transport;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;

public final class TransportSetup {

  private final SessionSetup sessionSetup;
  public final Serializer packetSerializer;

  public TransportSetup(final SessionSetup sessionSetup, final Serializer packetSerializer) {
    this.sessionSetup = Check.notNull(sessionSetup);
    this.packetSerializer = Check.notNull(packetSerializer);
  }

  /**
   * @param requestExecutor executes incoming requests
   */
  public TransportSetup(final Server server, final Serializer packetSerializer, final Executor requestExecutor, final SessionFactory sessionFactory) {
    this(new SessionSetup(server, requestExecutor, sessionFactory), packetSerializer);
  }

  public Session createSession(final Connection connection) throws Exception {
    return sessionSetup.createSession(connection);
  }

}
