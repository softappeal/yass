package ch.softappeal.yass.transport.ws.container;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class Client {

    public static final String HOST = "0.0.0.0";
    public static final int PORT = 8080;

    public static void run(final WebSocketContainer container) throws Exception {
        container.connectToServer(
            new Endpoint() {
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
                        final AtomicInteger counter = new AtomicInteger(0);

                        @Override
                        public void onMessage(final byte[] message) {
                            System.out.println("message: " + counter.getAndIncrement() + ", " + message.length);
                            try {
                                TimeUnit.MILLISECONDS.sleep(100);
                            } catch (final InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            },
            ClientEndpointConfig.Builder.create()
                .configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void beforeRequest(final Map<String, List<String>> headers) {
                        headers.put("Custom_" + System.currentTimeMillis(), Arrays.asList("foo"));
                    }
                })
                .build(),
            URI.create("ws://" + HOST + ":" + PORT + ApplicationConfig.WS_PATH)
        );
    }

}
