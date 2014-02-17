package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Check;

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class WsConnection extends Connection {

  private volatile Session session;
  private final Serializer packetSerializer;
  public final javax.websocket.Session wsSession;
  private final RemoteEndpoint.Basic remoteEndpoint;

  private static final boolean USE_STREAM = false; // $todo

  /**
   * @param createSessionExceptionHandler handles exceptions from {@link SessionFactory#create(SessionSetup, Connection)}
   */
  public WsConnection(
    final SessionSetup setup, final Serializer packetSerializer,
    final Thread.UncaughtExceptionHandler createSessionExceptionHandler,
    final javax.websocket.Session wsSession
  ) {
    // $todo: review
    this.packetSerializer = Check.notNull(packetSerializer);
    Check.notNull(createSessionExceptionHandler);
    this.wsSession = wsSession;
    remoteEndpoint = wsSession.getBasicRemote();
    try {
      session = setup.createSession(this);
    } catch (final Exception e) {
      try {
        // $todo session.close(CloseReason.CloseCodes.UNEXPECTED_CONDITION);
      } catch (final Exception e2) {
        e.addSuppressed(e2);
      }
      createSessionExceptionHandler.uncaughtException(Thread.currentThread(), e);
      session = null;
      return;
    }
    if (USE_STREAM) {
      wsSession.addMessageHandler(new MessageHandler.Whole<InputStream>() {
        @Override public void onMessage(final InputStream in) {
          try {
            final Packet packet;
            try {
              packet = (Packet)packetSerializer.read(Reader.create(in));
            } catch (final Exception e) {
              close(session, e);
              return;
            }
            received(session, packet);
            if (packet.isEnd()) {
              wsSession.close();
            }
          } catch (final IOException e) {
            throw new RuntimeException(e);
          }
        }
      });
    } else {
      wsSession.addMessageHandler(new MessageHandler.Whole<byte[]>() {
        @Override public void onMessage(final byte[] in) {
          try {
            final Packet packet;
            try {
              packet = (Packet)packetSerializer.read(Reader.create(new ByteArrayInputStream(in)));
            } catch (final Exception e) {
              close(session, e);
              return;
            }
            received(session, packet);
            if (packet.isEnd()) {
              wsSession.close();
            }
          } catch (final IOException e) {
            throw new RuntimeException(e);
          }

        }
      });
    }
    open(session); // $todo: check result
  }

  @Override protected void write(final Packet packet) throws Exception {
    // $todo: review
    if (USE_STREAM) {
      try (OutputStream out = remoteEndpoint.getSendStream()) {
        packetSerializer.write(packet, Writer.create(out));
      }
    } else {
      final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
      packetSerializer.write(packet, Writer.create(out));
      remoteEndpoint.sendBinary(ByteBuffer.wrap(out.toByteArray()));
    }
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

  void onError(final Throwable throwable) {
    close(session, (throwable == null) ? new Exception("throwable == null") : throwable);
  }

}
