package test;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public class JettyAcceptor {

    public static void main(final String... args) throws Exception {
        final Server server = new Server();
        final ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setHost(Initiator.HOST);
        serverConnector.setPort(Initiator.PORT);
        server.addConnector(serverConnector);
        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        server.setHandler(contextHandler);
        WebSocketServerContainerInitializer.configureContext(contextHandler).addEndpoint(ApplicationConfig.ENDPOINT_CONFIG);
        server.start();
    }

}
