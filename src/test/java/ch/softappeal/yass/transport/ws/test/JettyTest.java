package ch.softappeal.yass.transport.ws.test;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import java.util.concurrent.CountDownLatch;

public abstract class JettyTest extends WsTest {

    protected static void run(final CountDownLatch latch) throws Exception {
        final Server server = new Server();
        final ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setPort(PORT);
        server.addConnector(serverConnector);
        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        server.setHandler(contextHandler);
        WebSocketServerContainerInitializer.configureContext(contextHandler).addEndpoint(serverEndpointConfig());
        server.start();
        final ClientContainer container = new ClientContainer();
        container.start();
        connect(container, latch);
        container.stop();
        server.stop();
    }

}
