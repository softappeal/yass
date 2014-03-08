package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class WsConnection extends Connection {

  private volatile Session session;
  private final Serializer packetSerializer;
  public final javax.websocket.Session wsSession;
  private volatile RemoteEndpoint.Async remoteEndpoint;

  public WsConnection(final TransportSetup setup, final javax.websocket.Session wsSession) throws Exception {
    packetSerializer = setup.packetSerializer;
    this.wsSession = Check.notNull(wsSession);
    try {
      remoteEndpoint = wsSession.getAsyncRemote(); // $todo: implement batching ? setting send timeout ?
      session = setup.createSession(this);
    } catch (final Exception e) {
      try {
        wsSession.close();
      } catch (final Exception e2) {
        e.addSuppressed(e2);
      }
      throw e;
    }
    if (!open(session)) {
      return;
    }
    wsSession.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
      @Override public void onMessage(final ByteBuffer in) {
        try {
          received(session, (Packet)packetSerializer.read(Reader.create(in)));
          if (in.hasRemaining()) {
            throw new RuntimeException("input buffer is not empty");
          }
        } catch (final Exception e) {
          close(session, e);
        }
      }
    });
  }

  @Override protected void write(final Packet packet) throws Exception {
    new ByteArrayOutputStream(1024) {
      {
        packetSerializer.write(packet, Writer.create(this));
        remoteEndpoint.sendBinary(ByteBuffer.wrap(buf, 0, count), new SendHandler() {
          @Override public void onResult(final SendResult result) {
            if (result == null) {
              onError(new Exception("result == null"));
            } else if (!result.isOK()) {
              final Throwable throwable = result.getException();
              onError((throwable == null) ? new Exception("throwable == null") : throwable);
            }
          }
        });
      }
    };
  }

  @Override protected void closed() throws IOException {
    wsSession.close();
  }

  /**
   * Note: On {@link Session#close()}, {@link Session#closed(Throwable) Session.closed(null)} will be called before this method.
   */
  void onClose(final CloseReason closeReason) {
    onError(new Exception((closeReason == null) ? "closeReason == null" : closeReason.toString()));
  }

  /**
   * Note: session null check is needed if {@link SessionSetup#createSession(Connection)} call in constructor fails.
   */
  void onError(final Throwable throwable) {
    if (session != null) {
      close(session, (throwable == null) ? new Exception("throwable == null") : throwable);
    }
  }

}
