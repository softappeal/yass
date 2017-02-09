package ch.softappeal.yass.transport.ws.test.up;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class UserPropertiesTest {

    static int PORT = 9090;
    static String PATH = "/test";

    static ServerEndpointConfig.Configurator SERVER_CONFIGURATOR = new ServerEndpointConfig.Configurator() {
        @Override public <T> T getEndpointInstance(Class<T> endpointClass) {
            return endpointClass.cast(new Endpoint() {
                @Override public void onOpen(Session session, EndpointConfig config) {
                    System.out.printf(
                        "onOpen - config: %s, session: %s%n",
                        config.getUserProperties().keySet().stream().filter(k -> k.startsWith("Header")).collect(Collectors.toSet()),
                        session.getUserProperties().keySet().stream().filter(k -> k.startsWith("Header")).collect(Collectors.toSet())
                    );
                }
                @Override public void onClose(Session session, CloseReason closeReason) {
                }
                @Override public void onError(Session session, Throwable throwable) {
                }
            });
        }
        @Override public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            sec.getUserProperties().putAll(request.getHeaders());
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
    };

    static void clientConnect(WebSocketContainer container) throws Exception {
        for (String header : Arrays.asList("Header1", "Header2")) {
            container.connectToServer(
                new Endpoint() {
                    @Override public void onOpen(Session session, EndpointConfig config) {
                    }
                },
                ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator() {
                    @Override public void beforeRequest(Map<String, List<String>> headers) {
                        headers.put(header, Collections.singletonList("foo"));
                    }
                }).build(),
                URI.create("ws://localhost:" + PORT + PATH)
            );
            TimeUnit.SECONDS.sleep(1L);
        }
    }

}
