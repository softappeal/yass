package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.Writer.ByteBufferOutputStream;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class WsConnection implements Connection {

    @FunctionalInterface public interface Factory {
        WsConnection create(TransportSetup setup, Session session) throws Exception;
    }

    public final Session session;
    private final Serializer packetSerializer;
    private SessionClient sessionClient;

    protected WsConnection(final TransportSetup setup, final Session session) {
        packetSerializer = setup.packetSerializer;
        this.session = Check.notNull(session);
    }

    public static WsConnection create(final Factory connectionFactory, final TransportSetup setup, final Session session) throws Exception {
        final WsConnection connection = connectionFactory.create(setup, session);
        try {
            connection.created(session);
            connection.sessionClient = SessionClient.create(setup, connection);
            connection.sessionClient.opened();
        } catch (final Exception e) {
            try {
                session.close();
            } catch (final Exception e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }
        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() { // $note: this could be replaced with a lambda in WebSocket API 1.1
            @Override public void onMessage(final ByteBuffer in) {
                try {
                    connection.sessionClient.received((Packet)connection.packetSerializer.read(Reader.create(in)));
                    if (in.hasRemaining()) {
                        throw new RuntimeException("input buffer is not empty");
                    }
                } catch (final Exception e) {
                    connection.sessionClient.close(e);
                }
            }
        });
        return connection;
    }

    /**
     * Called after connection has been created.
     */
    protected abstract void created(Session session) throws Exception;

    protected final ByteBuffer writeToBuffer(final Packet packet) throws Exception {
        final ByteBufferOutputStream buffer = new ByteBufferOutputStream(128);
        packetSerializer.write(packet, Writer.create(buffer));
        return buffer.toByteBuffer();
    }

    @Override public final void closed() throws IOException {
        session.close();
    }

    final void onClose(final @Nullable CloseReason closeReason) {
        onError(new WsClosedException(closeReason));
    }

    final void onError(final @Nullable Throwable throwable) {
        sessionClient.close((throwable == null) ? new Exception("<no-throwable>") : throwable);
    }

}
