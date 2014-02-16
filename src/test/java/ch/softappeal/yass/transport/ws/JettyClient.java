package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.tutorial.client.ClientMain;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public final class JettyClient {

  private static final SessionSetup SESSION_SETUP = ClientMain.createSessionSetup(
    Executors.newCachedThreadPool(new NamedThreadFactory("requestExecutor", Exceptions.STD_ERR))
  );

  public static final class Endpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) {
      return new WsConnection(SESSION_SETUP, JettyServer.PACKET_SERIALIZER, Exceptions.STD_ERR, session);
    }
  }

  public static void main(final String... args) throws Exception {
    final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    container.connectToServer(new Endpoint(), null, JettyServer.THE_URI);// connect to node 1
    container.connectToServer(new Endpoint(), null, JettyServer.THE_URI);// connect to node 2 (simulated)
    System.out.println("started");
    new CountDownLatch(1).await();
  }

}
