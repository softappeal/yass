package ch.softappeal.yass.transport.ws.echo;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;

public final class JettyServer {

  public static final int PORT = 8080;
  public static final String PATH = "/echo";
  public static final URI THE_URI = URI.create("ws://localhost:" + PORT + PATH);

  public static void main(final String... args) throws Exception {
    final Server server = new Server(PORT);
    server.addConnector(new ServerConnector(server));
    final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    WebSocketServerContainerInitializer.configureContext(context).addEndpoint(
      ServerEndpointConfig.Builder.create(ServerEndpoint.class, PATH).build()
    );
    server.start();
  }

}
