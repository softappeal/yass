package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.serialize.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Writes to socket in caller thread.
 * Blocks if socket can't send data.
 */
public final class SyncSocketConnection extends SocketConnection {

    private SyncSocketConnection(final Serializer packetSerializer, final Socket socket, final OutputStream out) {
        super(packetSerializer, socket, out);
    }

    private final Object writeMutex = new Object();

    @Override public void write(final Packet packet) throws Exception {
        final ByteArrayOutputStream buffer = writeToBuffer(packet);
        synchronized (writeMutex) {
            flush(buffer);
        }
    }

    public static final Factory FACTORY = new Factory() {
        @Override public SocketConnection create(final Serializer packetSerializer, final Socket socket1, final OutputStream out) throws Exception {
            return new SyncSocketConnection(packetSerializer, socket1, out);
        }
    };

}
