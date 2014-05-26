package ch.softappeal.yass.tutorial.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.server.ServerEndpointConfig;
import java.net.InetSocketAddress;

public final class JettyServer extends WsServerSetup {

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
