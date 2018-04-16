package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.remote.session.Packet;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

/**
 * Sends messages synchronously.
 * Blocks if socket can't send data.
 */
public final class SyncWsConnection extends WsConnection {

    private SyncWsConnection(final WsConfigurator configurator, final Session session) {
        super(configurator, session);
        remoteEndpoint = session.getBasicRemote();
    }

    private final RemoteEndpoint.Basic remoteEndpoint;
    private final Object writeMutex = new Object();

    @Override public void write(final Packet packet) throws Exception {
        final var buffer = writeToBuffer(packet);
        synchronized (writeMutex) {
            remoteEndpoint.sendBinary(buffer);
        }
    }

    public static final Factory FACTORY = SyncWsConnection::new;

}
