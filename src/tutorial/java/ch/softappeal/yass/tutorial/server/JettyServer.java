package ch.softappeal.yass.tutorial.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public final class JettyServer extends WsServerSetup {

    public static void main(final String... args) throws Exception {
        final Server server = new Server();

        final ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setHost(HOST);
        serverConnector.setPort(PORT);
        server.addConnector(serverConnector);

        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        server.setHandler(contextHandler);

        WebSocketServerContainerInitializer.configureContext(contextHandler).addEndpoint(ENDPOINT_CONFIG);

        server.start();
        System.out.println("started");
    }

}
