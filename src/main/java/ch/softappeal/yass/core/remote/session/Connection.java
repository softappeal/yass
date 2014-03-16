package ch.softappeal.yass.core.remote.session;

public abstract class Connection {

  /**
   * Called once if the connection has been closed.
   */
  protected abstract void closed() throws Exception;

  /**
   * Called if a packet has to be written out.
   * Note: Calls to this method COULD happen concurrently.
   * Note: Could be called after close due to race conditions.
   */
  protected abstract void write(Packet packet) throws Exception;

  protected static void close(final SessionClient sessionClient, final Throwable throwable) {
    sessionClient.close(throwable);
  }

  protected static void received(final SessionClient sessionClient, final Packet packet) {
    sessionClient.received(packet);
  }

}
