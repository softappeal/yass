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
import java.nio.ByteBuffer;

public final class WsConnection extends Connection { // $todo: review

  private final Serializer packetSerializer;
  public final javax.websocket.Session session;
  private final RemoteEndpoint.Basic remoteEndpoint;

  /**
   * @param createSessionExceptionHandler handles exceptions from {@link SessionFactory#create(SessionSetup, Connection)}
   */
  public WsConnection(
    final SessionSetup setup, final Serializer packetSerializer,
    final Thread.UncaughtExceptionHandler createSessionExceptionHandler,
    final javax.websocket.Session session
  ) {
    this.packetSerializer = Check.notNull(packetSerializer);
    Check.notNull(createSessionExceptionHandler);
    this.session = session;
    remoteEndpoint = session.getBasicRemote();
    final Session yassSession;
    try {
      yassSession = setup.createSession(this);
    } catch (final Exception e) {
      try {
        // $todo session.close(CloseReason.CloseCodes.UNEXPECTED_CONDITION);
      } catch (final Exception e2) {
        e.addSuppressed(e2);
      }
      createSessionExceptionHandler.uncaughtException(Thread.currentThread(), e);
      return;
    }
    /*
    session.addMessageHandler(new MessageHandler.Whole<InputStream>() {
      @Override public void onMessage(final InputStream in) {
        try {
          final Packet packet;
          try {
            packet = (Packet)packetSerializer.read(Reader.create(in));
          } catch (final Exception e) {
            close(yassSession, e);
            return;
          }
          received(yassSession, packet);
          if (packet.isEnd()) {
            session.close();
          }
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
    */
    session.addMessageHandler(new MessageHandler.Whole<byte[]>(){
      @Override public void onMessage(final byte[] in) {
        try {
          final Packet packet;
          try {
            packet = (Packet)packetSerializer.read(Reader.create(new ByteArrayInputStream(in)));
          } catch (final Exception e) {
            close(yassSession, e);
            return;
          }
          received(yassSession, packet);
          if (packet.isEnd()) {
            session.close();
          }
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }

      }
    });
    open(yassSession); // $todo: check result
  }

  @Override protected void write(final Packet packet) throws Exception {
    /*
    try (OutputStream out = remoteEndpoint.getSendStream()) {
      packetSerializer.write(packet, Writer.create(out));
    }
    */
    final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
    packetSerializer.write(packet, Writer.create(out));
    remoteEndpoint.sendBinary(ByteBuffer.wrap(out.toByteArray()));
  }

  /**
   * Note: No more calls to {@link #write(Packet)} are accepted when this method is called due to implementation of {@link Session}.
   */
  @Override protected void closed() throws Exception {
    session.close();
  }

  void onClose(final CloseReason closeReason) {
    System.out.println("session '" + session.getId() + "' close: " + closeReason);
  }

  void onError(final Throwable throwable) {
    System.out.println("session '" + session.getId() + "' error: " + throwable);
  }

}
