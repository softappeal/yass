package ch.softappeal.yass.transport.ws.test;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import java.util.concurrent.CountDownLatch;

public abstract class JettyTest extends WsTest {

  protected static void run(final CountDownLatch latch) throws Exception {
    final Server server = new Server(PORT);
    server.addConnector(new ServerConnector(server));
    final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    WebSocketServerContainerInitializer.configureContext(context).addEndpoint(SERVER_ENDPOINT_CONFIG);
    server.start();
    final ClientContainer container = new ClientContainer();
    container.start();
    connect(container, latch);
    container.stop();
    server.stop();
  }

}
