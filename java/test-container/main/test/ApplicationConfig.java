package test;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ApplicationConfig implements ServerApplicationConfig {

    public static final String WS_PATH = "/ws";

    private static final Executor EXECUTOR = Executors.newCachedThreadPool();

    public static final ServerEndpointConfig ENDPOINT_CONFIG = ServerEndpointConfig.Builder
        .create(Endpoint.class, WS_PATH)
        .configurator(new ServerEndpointConfig.Configurator() {
            @Override public <T> T getEndpointInstance(final Class<T> endpointClass) {
                return endpointClass.cast(new Endpoint() {
                    void send(final RemoteEndpoint.Async remote) {
                        EXECUTOR.execute(() -> {
                            final long threadId = Thread.currentThread().getId();
                            System.out.println("start: " + threadId);
                            final long start = System.nanoTime();
                            for (int counter = 0; counter < 100; counter++) {
                                remote.sendBinary(
                                    ByteBuffer.wrap(new byte[50_000]),
                                    result -> {
                                        final long end = System.nanoTime();
                                        System.out.println("result: " + threadId + ", " + result.isOK() + ", " + ((end - start) / 1_000_000) + "ms");
                                    }
                                );
                            }
                            final long end = System.nanoTime();
                            System.out.println("end: " + threadId + ", " + ((end - start) / 1_000_000) + "ms");
                        });
                    }
                    public void onOpen(Session session, EndpointConfig config) {
                        final RemoteEndpoint.Async remote = session.getAsyncRemote();
                        send(remote);
                        send(remote);
                    }
                });
            }
        }).build();

    @Override public Set<ServerEndpointConfig> getEndpointConfigs(final Set<Class<? extends Endpoint>> endpointClasses) {
        System.out.println("ApplicationConfig");
        return Collections.singleton(ENDPOINT_CONFIG);
    }

    @Override public Set<Class<?>> getAnnotatedEndpointClasses(final Set<Class<?>> scanned) {
        return Collections.emptySet();
    }

}
