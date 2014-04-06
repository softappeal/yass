package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class WsConnection implements Connection {

  private final SessionClient sessionClient;
  private final Serializer packetSerializer;
  public final Session session;
  private final RemoteEndpoint.Async remoteEndpoint;

  public WsConnection(final TransportSetup setup, final Session session) throws Exception {
    packetSerializer = setup.packetSerializer;
    this.session = Check.notNull(session);
    try {
      remoteEndpoint = session.getAsyncRemote(); // $todo: implement batching ? setting send timeout ?
      sessionClient = new SessionClient(setup, this);
    } catch (final Exception e) {
      try {
        session.close();
      } catch (final Exception e2) {
        e.addSuppressed(e2);
      }
      throw e;
    }
    session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() { // $$$
      @Override public void onMessage(final ByteBuffer in) {
        try {
          sessionClient.received((Packet)packetSerializer.read(Reader.create(in)));
          if (in.hasRemaining()) {
            throw new RuntimeException("input buffer is not empty");
          }
        } catch (final Exception e) {
          sessionClient.close(e);
        }
      }
    });
  }

  @Override public void write(final Packet packet) throws Exception {
    new ByteArrayOutputStream(1024) {
      {
        packetSerializer.write(packet, Writer.create(this));
        remoteEndpoint.sendBinary(ByteBuffer.wrap(buf, 0, count), result -> {
          if (result == null) {
            onError(new Exception("result == null"));
          } else if (!result.isOK()) {
            final Throwable throwable = result.getException();
            onError((throwable == null) ? new Exception("throwable == null") : throwable);
          }
        });
      }
    };
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
