package ch.softappeal.yass.transport.ws.echo;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.server.ServerEndpointConfig;

public final class BinaryEchoJettyServer {

  public static void main(final String... args) throws Exception {
    final Server server = new Server(EchoJettyServer.PORT);
    server.addConnector(new ServerConnector(server));
    final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    WebSocketServerContainerInitializer.configureContext(context).addEndpoint(
      ServerEndpointConfig.Builder.create(BinaryEchoServerEndpoint.class, EchoJettyServer.PATH).build()
    );
    server.start();
  }

}
