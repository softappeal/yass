package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.Writer.ByteBufferOutputStream;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class WsConnection implements Connection {

    private final Serializer packetSerializer;
    public final javax.websocket.Session session;
    private Session yassSession;

    protected WsConnection(final Serializer packetSerializer, final javax.websocket.Session session) {
        this.packetSerializer = Check.notNull(packetSerializer);
        this.session = Check.notNull(session);
    }

    protected final ByteBuffer writeToBuffer(final Packet packet) throws Exception {
        final ByteBufferOutputStream buffer = new ByteBufferOutputStream(128);
        packetSerializer.write(packet, Writer.create(buffer));
        return buffer.toByteBuffer();
    }

    final void onClose(final CloseReason closeReason) {
        if (closeReason.getCloseCode().getCode() != CloseReason.CloseCodes.NORMAL_CLOSURE.getCode()) {
            onError(new RuntimeException("WebSocket closed - " + closeReason.toString()));
        }
    }

    static Exception wrap(final @Nullable Throwable throwable) {
        if (throwable == null) {
            return new Exception("<no-throwable>");
        } else if (throwable instanceof Exception) {
            return (Exception)throwable;
        } else if (throwable instanceof Error) {
            throw (Error)throwable;
        } else {
            throw new Error(throwable);
        }
    }

    protected final void onError(final @Nullable Throwable ignore) {
        Session.close(yassSession, wrap(ignore));
    }

    @Override public final void closed() throws IOException {
        session.close();
    }

    private void created() {
        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() { // $note: this could be replaced with a lambda in WebSocket API 1.1
            @Override public void onMessage(final ByteBuffer in) {
                try {
                    Session.received(yassSession, (Packet)packetSerializer.read(Reader.create(in)));
                    if (in.hasRemaining()) {
                        throw new RuntimeException("input buffer is not empty");
                    }
                } catch (final Exception ignore) {
                    Session.close(yassSession, ignore);
                }
            }
        });
    }

    @FunctionalInterface public interface Factory {
        WsConnection create(Serializer packetSerializer, javax.websocket.Session session) throws Exception;
    }

    public static WsConnection create(final Factory connectionFactory, final TransportSetup setup, final javax.websocket.Session session) throws Exception {
        try {
            final WsConnection connection = connectionFactory.create(setup.packetSerializer, session);
            connection.created();
            connection.yassSession = Session.create(setup.sessionFactory, connection);
            return connection;
        } catch (final Exception e) {
            try {
                session.close();
            } catch (final Exception e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }
    }

}
