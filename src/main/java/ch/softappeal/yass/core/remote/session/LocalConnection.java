package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Exceptions;

public final class LocalConnection extends Connection {

  private SessionClient sessionClient;

  private LocalConnection() {
    // disable
  }

  @Override protected void write(final Packet packet) {
    received(sessionClient, packet);
  }

  @Override protected void closed() {
    sessionClient.close();
  }

  /**
   * Creates two connected local sessions.
   */
  public static void connect(final SessionSetup setup1, final SessionSetup setup2) {
    final LocalConnection connection1 = new LocalConnection();
    final LocalConnection connection2 = new LocalConnection();
    try {
      connection2.sessionClient = new SessionClient(setup1, connection1);
      try {
        connection1.sessionClient = new SessionClient(setup2, connection2);
      } catch (final Exception e) {
        try {
          connection2.sessionClient.close(e);
        } catch (final Exception e2) {
          // e.addSuppressed(e2);
        }
        throw e;
      }
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
  }

}
