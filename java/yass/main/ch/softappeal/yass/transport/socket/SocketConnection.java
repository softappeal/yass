package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.remote.session.Connection;
import ch.softappeal.yass.remote.session.Packet;
import ch.softappeal.yass.remote.session.Session;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.TransportSetup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;

public abstract class SocketConnection implements Connection {

    private final Serializer packetSerializer;
    public final Socket socket;
    private final OutputStream out;

    protected SocketConnection(final Serializer packetSerializer, final Socket socket, final OutputStream out) {
        this.packetSerializer = Objects.requireNonNull(packetSerializer);
        this.socket = Objects.requireNonNull(socket);
        this.out = Objects.requireNonNull(out);
    }

    protected final ByteArrayOutputStream writeToBuffer(final Packet packet) throws Exception {
        final var buffer = new ByteArrayOutputStream(128);
        packetSerializer.write(packet, Writer.create(buffer));
        return buffer;
    }

    /**
     * Buffering of output is needed to prevent long delays due to Nagle's algorithm.
     */
    protected final void flush(final ByteArrayOutputStream buffer) throws IOException {
        buffer.writeTo(out);
        out.flush();
    }

    @Override public void closed() throws Exception {
        socket.close();
    }

    /**
     * Called after connection has been created.
     */
    protected void created(final Session session) throws Exception {
        // empty
    }

    @FunctionalInterface public interface Factory {
        SocketConnection create(Serializer packetSerializer, Socket socket, OutputStream out) throws Exception;
    }

    static void create(final Factory connectionFactory, final TransportSetup setup, final Socket socket, final Reader reader, final OutputStream out) throws Exception {
        final var connection = connectionFactory.create(setup.packetSerializer, socket, out);
        final var session = Session.create(setup.sessionFactory, connection);
        try {
            connection.created(session);
            while (true) {
                final var packet = (Packet)connection.packetSerializer.read(reader);
                Session.received(session, packet);
                if (packet.isEnd()) {
                    return;
                }
            }
        } catch (final Exception e) {
            Session.close(session, e);
        }
    }

}
