package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.remote.session.Packet;
import ch.softappeal.yass.serialize.Serializer;

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
        final var buffer = writeToBuffer(packet);
        synchronized (writeMutex) {
            flush(buffer);
        }
    }

    public static final Factory FACTORY = SyncSocketConnection::new;

}
