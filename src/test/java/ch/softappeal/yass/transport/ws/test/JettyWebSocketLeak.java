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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JettyWebSocketLeak {

    public static class WsConfigurator extends ServerEndpointConfig.Configurator {
        private final String side;
        public WsConfigurator(String side) {
            this.side = side;
        }
        @SuppressWarnings("unchecked")
        @Override public <T> T getEndpointInstance(Class<T> endpointClass) {
            return (T)new Endpoint() {
                @Override public void onOpen(Session session, EndpointConfig config) {
                    System.out.println("opening " + side + " " + session.hashCode());
                    if ("client".equals(side)) {
                        try {
                            TimeUnit.SECONDS.sleep(5);
                            System.out.println("closing " + side + " " + session.hashCode());
                            session.close();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                @Override public void onClose(Session session, CloseReason closeReason) {
                    System.out.println("closed " + side + " " + closeReason + " " + session.hashCode());
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
        clientContainer.connectToServer(new WsConfigurator("client").getEndpointInstance(Endpoint.class), ClientEndpointConfig.Builder.create().build(), THE_URI);
        TimeUnit.SECONDS.sleep(5);
        System.out.println();
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
        serverContainer.addEndpoint(ServerEndpointConfig.Builder.create(Endpoint.class, PATH).configurator(new WsConfigurator("server")).build());
        server.start();
        connectClient();
        connectClient();
        System.out.println("printing leaked server sessions ...");
        serverContainer.getBeans(WebSocketServerFactory.class).forEach(factory -> factory.getBeans(JsrSession.class).forEach(session -> System.out.println(session.hashCode())));
    }

    /* program output:

    opening client 1460681129
    opening server 1235546038
    closing client 1460681129
    closed client CloseReason[1005] 1460681129
    closed server CloseReason[1005] 1235546038

    opening server 1421996380
    opening client 1389119041
    closing client 1389119041
    closed client CloseReason[1005] 1389119041
    closed server CloseReason[1005] 1421996380

    printing leaked server sessions ...
    1235546038
    1421996380

     */

}
