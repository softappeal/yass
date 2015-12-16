package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.serialize.Serializer;

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

    private SyncWsConnection(final Serializer packetSerializer, final Session session) {
        super(packetSerializer, session);
        remoteEndpoint = session.getBasicRemote();
    }

    private final RemoteEndpoint.Basic remoteEndpoint;
    private final Object writeMutex = new Object();

    @Override public void write(final Packet packet) throws Exception {
        final ByteBuffer buffer = writeToBuffer(packet);
        synchronized (writeMutex) {
            remoteEndpoint.sendBinary(buffer);
        }

    }

}
