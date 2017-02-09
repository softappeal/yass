package ch.softappeal.yass.transport.ws.test.up;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;

public class JettyUserPropertiesTest extends UserPropertiesTest {

    public static void main(String... args) throws Exception {
        Server server = new Server();
        ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setPort(PORT);
        server.addConnector(serverConnector);
        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setContextPath("/");
        server.setHandler(servletContextHandler);
        ServerContainer serverContainer = WebSocketServerContainerInitializer.configureContext(servletContextHandler);
        serverContainer.addEndpoint(ServerEndpointConfig.Builder.create(Endpoint.class, PATH).configurator(SERVER_CONFIGURATOR).build());
        server.start();
        ClientContainer clientContainer = new ClientContainer();
        clientContainer.start();
        clientConnect(clientContainer);
    }

}
