package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.server.JettyServer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;

import javax.websocket.Session;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public final class JettyClient { // $todo: review

  private static final SessionSetup SESSION_SETUP = SocketClient.createSessionSetup(
    Executors.newCachedThreadPool(new NamedThreadFactory("requestExecutor", Exceptions.STD_ERR))
  );

  public static final class Endpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) {
      return new WsConnection(SESSION_SETUP, Config.PACKET_SERIALIZER, Exceptions.STD_ERR, session);
    }
  }

  public static void main(final String... args) throws Exception {
    final ClientContainer container = new ClientContainer();
    container.start();
    container.connectToServer(new Endpoint(), null, JettyServer.THE_URI);// connect to node 1
    container.connectToServer(new Endpoint(), null, JettyServer.THE_URI);// connect to node 2 (simulated)
    System.out.println("started");
    new CountDownLatch(1).await();
  }

}
