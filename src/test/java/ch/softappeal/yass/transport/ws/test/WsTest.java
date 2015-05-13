package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.core.remote.session.test.LocalConnectionTest;
import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.test.SocketPerformanceTest;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.After;
import org.junit.Before;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class WsTest {

    protected static final int PORT = 9090;
    protected static final String PATH = "/test";
    protected static final URI THE_URI = URI.create("ws://localhost:" + PORT + PATH);

    private static volatile ExecutorService DISPATCHER_EXECUTOR;

    @Before public void startDispatcherExecutor() {
        DISPATCHER_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("dispatcherExecutor", Exceptions.TERMINATE));
    }

    @After public void stopDispatcherExecutor() {
        DISPATCHER_EXECUTOR.shutdown();
    }

    private static volatile TransportSetup TRANSPORT_SETUP_CLIENT;
    private static volatile TransportSetup TRANSPORT_SETUP_SERVER;

    public static final class ClientEndpoint extends WsEndpoint {
        @Override protected WsConnection createConnection(final Session session) throws Exception {
            return WsConnection.create(TRANSPORT_SETUP_CLIENT, session);
        }
    }

    public static final class ServerEndpoint extends WsEndpoint {
        @Override protected WsConnection createConnection(final Session session) throws Exception {
            return WsConnection.create(TRANSPORT_SETUP_SERVER, session);
        }
    }

    protected static ServerEndpointConfig serverEndpointConfig(final ServerEndpointConfig.Configurator configurator) {
        return ServerEndpointConfig.Builder.create(ServerEndpoint.class, PATH).configurator(configurator).build();
    }

    protected static void setTransportSetup(
        final boolean serverInvoke, final boolean serverCreateException,
        final boolean clientInvoke, final boolean clientCreateException
    ) {
        TRANSPORT_SETUP_SERVER = LocalConnectionTest.createSetup(serverInvoke, DISPATCHER_EXECUTOR, serverCreateException);
        TRANSPORT_SETUP_CLIENT = LocalConnectionTest.createSetup(clientInvoke, DISPATCHER_EXECUTOR, clientCreateException);
    }

    protected static void setPerformanceSetup(final CountDownLatch latch) {
        TRANSPORT_SETUP_SERVER = PerformanceTest.createSetup(DISPATCHER_EXECUTOR, null, SocketPerformanceTest.COUNTER);
        TRANSPORT_SETUP_CLIENT = PerformanceTest.createSetup(DISPATCHER_EXECUTOR, latch, SocketPerformanceTest.COUNTER);
    }

    protected static void connect(final WebSocketContainer container, final CountDownLatch latch) throws Exception {
        container.connectToServer(new ClientEndpoint(), ClientEndpointConfig.Builder.create().build(), THE_URI);
        latch.await();
        TimeUnit.MILLISECONDS.sleep(400L);
    }

}
