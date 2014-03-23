package ch.softappeal.yass.core.remote.session;

public interface Connection {

  /**
   * Called once if the connection has been closed.
   */
  void closed() throws Exception;

  /**
   * Called if a packet has to be written out.
   * Note: Calls to this method COULD happen concurrently.
   * Note: Could be called after close due to race conditions.
   */
  void write(Packet packet) throws Exception;

}
