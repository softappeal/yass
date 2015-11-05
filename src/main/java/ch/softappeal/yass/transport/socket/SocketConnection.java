package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public abstract class SocketConnection implements Connection {

    @FunctionalInterface public interface Factory {
        SocketConnection create(TransportSetup setup, Socket socket, OutputStream out) throws Exception;
    }

    static void create(final Factory connectionFactory, final TransportSetup setup, final Socket socket, final Reader reader, final OutputStream out) throws Exception {
        final SocketConnection connection = connectionFactory.create(setup, socket, out);
        final SessionClient sessionClient = SessionClient.create(setup, connection);
        sessionClient.opened();
        try {
            connection.created(sessionClient);
        } catch (final Exception e) {
            sessionClient.close(e);
            return;
        }
        connection.read(sessionClient, reader);
    }

    private final Serializer packetSerializer;
    public final Socket socket;
    protected final OutputStream out;

    protected SocketConnection(final TransportSetup setup, final Socket socket, final OutputStream out) {
        this.packetSerializer = setup.packetSerializer;
        this.socket = Check.notNull(socket);
        this.out = Check.notNull(out);
    }

    /**
     * Called after connection has been created.
     */
    protected void created(SessionClient sessionClient) throws Exception {
        // empty
    }

    private void read(final SessionClient sessionClient, final Reader reader) {
        while (true) {
            try {
                final Packet packet = (Packet)packetSerializer.read(reader);
                sessionClient.received(packet);
                if (packet.isEnd()) {
                    return;
                }
            } catch (final Exception e) {
                sessionClient.close(e);
                return;
            }
        }
    }

    protected final ByteArrayOutputStream writeToBuffer(final Packet packet) throws Exception {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(128);
        packetSerializer.write(packet, Writer.create(buffer));
        return buffer;
    }

    @Override public void closed() throws Exception {
        socket.close();
    }

    /**
     * Buffering of output is needed to prevent long delays due to Nagle's algorithm.
     */
    protected static void flush(final ByteArrayOutputStream buffer, final OutputStream out) throws IOException {
        buffer.writeTo(out);
        out.flush();
    }

}
