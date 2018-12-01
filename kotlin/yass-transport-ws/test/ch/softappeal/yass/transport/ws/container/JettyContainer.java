package ch.softappeal.yass.transport.ws.container;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.websocket.jsr356.server.deploy.*;

public class JettyContainer {

    public static void main(final String... args) throws Exception {
        final Server server = new Server();
        final ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setHost(Client.HOST);
        serverConnector.setPort(Client.PORT);
        server.addConnector(serverConnector);
        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        server.setHandler(contextHandler);
        WebSocketServerContainerInitializer
            .configureContext(contextHandler)
            .addEndpoint(ApplicationConfig.ENDPOINT_CONFIG);
        server.start();
    }

}
