package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Exceptions;

public final class LocalConnection extends Connection {

  @SuppressWarnings("InstanceVariableMayNotBeInitialized") private Session other;

  private LocalConnection() {
    // disable
  }

  @Override protected void write(final Packet packet) {
    received(other, packet);
  }

  @Override protected void closed() {
    other.close();
  }

  /**
   * Creates two connected local sessions.
   */
  public static void connect(final SessionSetup setup1, final SessionSetup setup2) {
    final LocalConnection connection1 = new LocalConnection();
    final LocalConnection connection2 = new LocalConnection();
    try {
      connection2.other = setup1.createSession(connection1);
      try {
        connection1.other = setup2.createSession(connection2);
      } catch (final Exception e) {
        try {
          connection2.other.close(e);
        } catch (final Exception e2) {
          e.addSuppressed(e2);
        }
        throw e;
      }
      if (!(open(connection1.other) && open(connection2.other))) {
        throw new RuntimeException("open failed");
      }
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
  }

}
