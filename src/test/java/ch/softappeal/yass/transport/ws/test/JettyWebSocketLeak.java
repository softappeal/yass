package ch.softappeal.yass.transport.ws.test;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.eclipse.jetty.websocket.jsr356.JsrSession;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class JettyWebSocketLeak {

    public static class WsConfigurator extends ServerEndpointConfig.Configurator {
        @SuppressWarnings("unchecked")
        @Override public <T> T getEndpointInstance(Class<T> endpointClass) {
            return (T)new Endpoint() {
                @Override public void onOpen(Session session, EndpointConfig config) {
                    try {
                        System.out.println("closing " + session);
                        session.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                @Override public void onClose(Session session, CloseReason closeReason) {
                }
                @Override public void onError(Session session, Throwable throwable) {
                }
            };
        }
        @Override public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
            return "";
        }
        @Override public List<Extension> getNegotiatedExtensions(List<Extension> installed, List<Extension> requested) {
            return new ArrayList<>();
        }
        @Override public boolean checkOrigin(String originHeaderValue) {
            return true;
        }
        @Override public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        }
    }

    private static int PORT = 9090;
    private static String PATH = "/test";
    private static URI THE_URI = URI.create("ws://localhost:" + PORT + PATH);

    private static void connectClient() throws Exception {
        ClientContainer clientContainer = new ClientContainer();
        clientContainer.start();
        clientContainer.connectToServer(new WsConfigurator().getEndpointInstance(Endpoint.class), ClientEndpointConfig.Builder.create().build(), THE_URI);
        clientContainer.stop();
    }

    public static void main(String... args) throws Exception {
        Server server = new Server();
        ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setPort(PORT);
        server.addConnector(serverConnector);
        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setContextPath("/");
        server.setHandler(servletContextHandler);
        ServerContainer serverContainer = WebSocketServerContainerInitializer.configureContext(servletContextHandler);
        serverContainer.addEndpoint(ServerEndpointConfig.Builder.create(Endpoint.class, PATH).configurator(new WsConfigurator()).build());
        server.start();
        connectClient();
        connectClient();
        System.out.println("printing the leaked server side sessions ...");
        serverContainer.getBeans(WebSocketServerFactory.class).forEach(factory -> factory.getBeans(JsrSession.class).forEach(System.out::println));
    }

}
