package ch.softappeal.yass.tutorial.server;

import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class JettyServer {

  public static final String HOST = "localhost";
  public static final int PORT = 9090;
  public static final String PATH = "/tutorial";

  public static final Executor REQUEST_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("requestExecutor", Exceptions.STD_ERR));

  private static final TransportSetup TRANSPORT_SETUP = SocketServer.createTransportSetup(REQUEST_EXECUTOR);

  public static final class Endpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) throws Exception {
      return new WsConnection(TRANSPORT_SETUP, session);
    }
  }

  public static void main(final String... args) throws Exception {
    final Server server = new Server(new InetSocketAddress(HOST, PORT));
    server.addConnector(new ServerConnector(server));

    final ServletContextHandler webSocketHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    webSocketHandler.setContextPath("/");

    final ResourceHandler resourceHandler = new ResourceHandler();
    resourceHandler.setDirectoriesListed(true);
    resourceHandler.setResourceBase(".");

    final HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] {resourceHandler, webSocketHandler});
    server.setHandler(handlers);

    WebSocketServerContainerInitializer.configureContext(webSocketHandler).addEndpoint(
      ServerEndpointConfig.Builder.create(Endpoint.class, PATH).build()
    );

    server.start();
    System.out.println("started");
  }

}
