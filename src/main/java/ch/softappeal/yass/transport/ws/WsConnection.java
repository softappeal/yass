package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.Writer.ByteBufferOutputStream;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class WsConnection implements Connection {

    private final Serializer packetSerializer;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    public final javax.websocket.Session session;
    private Session yassSession;

    protected WsConnection(final WsConfigurator configurator, final javax.websocket.Session session) {
        packetSerializer = configurator.setup.packetSerializer;
        uncaughtExceptionHandler = configurator.uncaughtExceptionHandler;
        this.session = Check.notNull(session);
    }

    protected final ByteBuffer writeToBuffer(final Packet packet) throws Exception {
        final ByteBufferOutputStream buffer = new ByteBufferOutputStream(128);
        packetSerializer.write(packet, Writer.create(buffer));
        return buffer.toByteBuffer();
    }

    final void onClose(final CloseReason closeReason) {
        if (closeReason.getCloseCode().getCode() == CloseReason.CloseCodes.NORMAL_CLOSURE.getCode()) {
            yassSession.close();
        } else {
            onError(new RuntimeException("WebSocket closed - " + closeReason.toString()));
        }
    }

    protected final void onError(final @Nullable Throwable ignore) {
        if (ignore == null) {
            Session.close(yassSession, new Exception("<no-throwable>"));
        } else if (ignore instanceof Exception) {
            Session.close(yassSession, (Exception)ignore);
        } else {
            Exceptions.uncaughtException(uncaughtExceptionHandler, ignore);
        }
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
        WsConnection create(WsConfigurator configurator, javax.websocket.Session session) throws Exception;
    }

    static WsConnection create(final WsConfigurator configurator, final javax.websocket.Session session) throws Exception {
        try {
            final WsConnection connection = configurator.connectionFactory.create(configurator, session);
            connection.created();
            connection.yassSession = Session.create(configurator.setup.sessionFactory, connection);
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
