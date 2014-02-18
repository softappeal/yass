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
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class WsConnection extends Connection {

  private volatile Session session;
  private final Serializer packetSerializer;
  public final javax.websocket.Session wsSession;
  private final RemoteEndpoint.Async remoteEndpoint;

  /**
   * @param createSessionExceptionHandler handles exceptions from {@link SessionFactory#create(SessionSetup, Connection)}
   */
  public WsConnection(
    final SessionSetup setup, final Serializer packetSerializer,
    final Thread.UncaughtExceptionHandler createSessionExceptionHandler,
    final javax.websocket.Session wsSession
  ) {
    this.packetSerializer = Check.notNull(packetSerializer);
    Check.notNull(createSessionExceptionHandler);
    this.wsSession = wsSession;
    remoteEndpoint = wsSession.getAsyncRemote(); // $todo: implement batching ? setting send timeout ?
    try {
      session = setup.createSession(this);
    } catch (final Exception e) {
      try {
        wsSession.close();
      } catch (final Exception e2) {
        e.addSuppressed(e2);
      }
      createSessionExceptionHandler.uncaughtException(Thread.currentThread(), e);
      return;
    }
    if (!open(session)) {
      return;
    }
    wsSession.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
      @Override public void onMessage(final ByteBuffer in) {
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
  }

  @Override protected void write(final Packet packet) throws Exception {
    new ByteArrayOutputStream(1024) {
      {
        packetSerializer.write(packet, Writer.create(this));
        remoteEndpoint.sendBinary(ByteBuffer.wrap(buf, 0, count), new SendHandler() {
          @Override public void onResult(final SendResult result) {
            if (result == null) {
              onError(new Exception("result == null"));
            } else {
              final Throwable throwable = result.getException();
              if (throwable != null) {
                onError(throwable);
              }
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
