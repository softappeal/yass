package ch.softappeal.yass.transport.ws.echo;

import org.eclipse.jetty.websocket.jsr356.ClientContainer;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public final class BinaryEchoJettyClient {

  public static void run(final WebSocketContainer container) throws Exception {
    final ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
    {
      final Session session = container.connectToServer(new BinaryEchoClientEndpoint(), config, EchoJettyServer.THE_URI);
      session.getBasicRemote().sendBinary(ByteBuffer.wrap(new byte[] {}));
      session.getBasicRemote().sendBinary(ByteBuffer.wrap(new byte[] {1, 2, 3}));
      TimeUnit.MILLISECONDS.sleep(100);
      session.close();
    }
    {
      final Session session = container.connectToServer(new BinaryEchoClientEndpoint(), config, EchoJettyServer.THE_URI);
      session.getBasicRemote().sendBinary(ByteBuffer.wrap(new byte[] {3}));
      session.getBasicRemote().sendBinary(ByteBuffer.wrap(new byte[] {0}));
      TimeUnit.MILLISECONDS.sleep(100);
    }
    {
      final Session session = container.connectToServer(new BinaryEchoClientEndpoint(), config, EchoJettyServer.THE_URI);
      session.getBasicRemote().sendBinary(ByteBuffer.wrap(new byte[] {3}));
      session.getBasicRemote().sendBinary(ByteBuffer.wrap(new byte[] {1}));
      TimeUnit.MILLISECONDS.sleep(100);
    }
    {
      final Session session = container.connectToServer(new BinaryEchoClientEndpoint(), config, EchoJettyServer.THE_URI);
      session.getBasicRemote().sendBinary(ByteBuffer.wrap(new byte[] {3}));
      session.getBasicRemote().sendBinary(ByteBuffer.wrap(new byte[] {2}));
      TimeUnit.MILLISECONDS.sleep(100);
    }
  }

  public static void main(final String... args) throws Exception {
    final ClientContainer container = new ClientContainer();
    container.start();
    run(container);
  }

}
