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

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class WsConnection implements Connection {

    public final Session session;
    private final Serializer packetSerializer;
    private RemoteEndpoint.Async remoteEndpoint;
    private SessionClient sessionClient;

    private WsConnection(final TransportSetup setup, final Session session) {
        packetSerializer = setup.packetSerializer;
        this.session = Check.notNull(session);
    }

    public static WsConnection create(final TransportSetup setup, final Session session) throws Exception {
        final WsConnection connection = new WsConnection(setup, session);
        try {
            connection.remoteEndpoint = session.getAsyncRemote();
            connection.sessionClient = SessionClient.create(setup, connection);
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

    @Override public void write(final Packet packet) throws Exception {
        final ByteBufferOutputStream out = new ByteBufferOutputStream(1024);
        packetSerializer.write(packet, Writer.create(out));
        remoteEndpoint.sendBinary(out.toByteBuffer(), result -> {
            if (result == null) {
                onError(new Exception("result == null"));
            } else if (!result.isOK()) {
                final Throwable throwable = result.getException();
                onError((throwable == null) ? new Exception("throwable == null") : throwable);
            }
        });
    }

    @Override public void closed() throws IOException {
        session.close();
    }

    void onClose(final CloseReason closeReason) {
        onError(new Exception((closeReason == null) ? "closeReason == null" : closeReason.toString()));
    }

    void onError(final Throwable throwable) {
        sessionClient.close((throwable == null) ? new Exception("throwable == null") : throwable);
    }

}
