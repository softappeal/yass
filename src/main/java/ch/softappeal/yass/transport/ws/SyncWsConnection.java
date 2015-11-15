package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.transport.TransportSetup;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.nio.ByteBuffer;

/**
 * Sends messages synchronously.
 * Blocks if socket can't send data.
 */
public final class SyncWsConnection extends WsConnection {

    public static final Factory FACTORY = new Factory() {
        @Override public WsConnection create(final TransportSetup setup, final Session session) throws Exception {
            return new SyncWsConnection(setup, session);
        }
    };

    private RemoteEndpoint.Basic remoteEndpoint;

    private SyncWsConnection(final TransportSetup setup, final Session session) {
        super(setup, session);
    }

    @Override protected void created(final Session session) {
        remoteEndpoint = session.getBasicRemote();
    }

    private final Object writeMutex = new Object();

    @Override public void write(final Packet packet) throws Exception {
        final ByteBuffer buffer = writeToBuffer(packet);
        synchronized (writeMutex) {
            remoteEndpoint.sendBinary(buffer);
        }

    }

}
