package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.transport.TransportSetup;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Writes to socket in caller thread.
 * Blocks if socket can't send data.
 */
public final class SyncSocketConnection extends SocketConnection {

    public static final Factory FACTORY = new Factory() {
        @Override public SocketConnection create(final TransportSetup setup, final Socket socket, final OutputStream out) throws Exception {
            return new SyncSocketConnection(setup, socket, out);
        }
    };

    private final Object writeMutex = new Object();

    private SyncSocketConnection(final TransportSetup setup, final Socket socket, final OutputStream out) {
        super(setup, socket, out);
    }

    @Override public void write(final Packet packet) throws Exception {
        final ByteArrayOutputStream buffer = writeToBuffer(packet);
        synchronized (writeMutex) {
            flush(buffer, out);
        }
    }

}
