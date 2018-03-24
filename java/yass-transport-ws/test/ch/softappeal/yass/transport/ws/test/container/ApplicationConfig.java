package ch.softappeal.yass.transport.ws.test.container;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ApplicationConfig implements ServerApplicationConfig {

    public static final String WS_PATH = "/ws";

    private static final Executor EXECUTOR = Executors.newCachedThreadPool();

    public static final ServerEndpointConfig ENDPOINT_CONFIG = ServerEndpointConfig.Builder
        .create(Endpoint.class, WS_PATH)
        .configurator(new ServerEndpointConfig.Configurator() {
            @Override public void modifyHandshake(final ServerEndpointConfig sec, final HandshakeRequest request, final HandshakeResponse response) {
                // note: works in Tomcat and Jetty but not in Undertow
                //     http://dev.eclipse.org/mhonarc/lists/jetty-users/msg07615.html
                //     http://lists.jboss.org/pipermail/undertow-dev/2017-February/001892.html
                //     https://java.net/jira/browse/WEBSOCKET_SPEC-218
                //     https://java.net/jira/browse/WEBSOCKET_SPEC-235
                //     http://stackoverflow.com/questions/17936440/accessing-httpsession-from-httpservletrequest-in-a-web-socket-serverendpoint/17994303#17994303
                sec.getUserProperties().putAll(request.getHeaders());
            }
            @Override public <T> T getEndpointInstance(final Class<T> endpointClass) {
                return endpointClass.cast(new Endpoint() {
                    void send(final RemoteEndpoint.Async remote) {
                        EXECUTOR.execute(() -> {
                            final var threadId = Thread.currentThread().getId();
                            System.out.println("start: " + threadId);
                            final var start = System.nanoTime();
                            for (var counter = 0; counter < 1; counter++) {
                                remote.sendBinary(
                                    ByteBuffer.wrap(new byte[50_000]),
                                    result -> {
                                        final var end = System.nanoTime();
                                        System.out.println("result: " + threadId + ", " + result.isOK() + ", " + ((end - start) / 1_000_000) + "ms");
                                    }
                                );
                            }
                            final var end = System.nanoTime();
                            System.out.println("end: " + threadId + ", " + ((end - start) / 1_000_000) + "ms");
                        });
                    }
                    public void onOpen(Session session, EndpointConfig config) {
                        System.out.println("getUserProperties: " + session.getUserProperties().keySet());
                        final var remote = session.getAsyncRemote();
                        send(remote);
                        send(remote);
                    }
                });
            }
        }).build();

    @Override public Set<ServerEndpointConfig> getEndpointConfigs(final Set<Class<? extends Endpoint>> endpointClasses) {
        System.out.println("ApplicationConfig");
        return Set.of(ENDPOINT_CONFIG);
    }

    @Override public Set<Class<?>> getAnnotatedEndpointClasses(final Set<Class<?>> scanned) {
        return Set.of();
    }

}
