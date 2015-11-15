package ch.softappeal.yass.core.remote.session;

public interface Connection {

    /**
     * Called if a packet has to be written out. Must be thread safe.
     */
    void write(Packet packet) throws Exception;

    /**
     * Called once if the connection has been closed.
     */
    void closed() throws Exception;

}
