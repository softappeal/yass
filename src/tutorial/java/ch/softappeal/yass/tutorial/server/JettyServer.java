package ch.softappeal.yass.tutorial.server;

import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.util.concurrent.Executors;

public final class JettyServer { // $todo: review

  private static final int PORT = 9090;
  private static final String PATH = "/tutorial";
  public static final URI THE_URI = URI.create("ws://localhost:" + PORT + PATH);

  private static final SessionSetup SESSION_SETUP = SocketServer.createSessionSetup(
    Executors.newCachedThreadPool(new NamedThreadFactory("requestExecutor", Exceptions.STD_ERR))
  );

  public static final class Endpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) {
      return new WsConnection(SESSION_SETUP, Config.PACKET_SERIALIZER, Exceptions.STD_ERR, session);
    }
  }

  public static void main(final String... args) throws Exception {
    final Server server = new Server(PORT);
    server.addConnector(new ServerConnector(server));
    final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    WebSocketServerContainerInitializer.configureContext(context).addEndpoint(
      ServerEndpointConfig.Builder.create(Endpoint.class, PATH).build()
    );
    server.start();
  }

}
