package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.NamedThreadFactory;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.transport.ws.AsyncWsConnection;
import ch.softappeal.yass.transport.ws.SyncWsConnection;
import ch.softappeal.yass.transport.ws.WsConfigurator;
import org.junit.After;
import org.junit.Before;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class WsTest extends TransportTest {

    protected static final int PORT = 9090;
    protected static final String PATH = "/test";
    protected static final URI THE_URI = URI.create("ws://localhost:" + PORT + PATH);

    private static ExecutorService INITIATOR_EXECUTOR;
    private static ExecutorService ACCEPTOR_EXECUTOR;

    @Before public void startDispatcherExecutor() {
        INITIATOR_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("initiatorExecutor", Exceptions.TERMINATE));
        ACCEPTOR_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("acceptorExecutor", Exceptions.TERMINATE));
    }

    @After public void stopDispatcherExecutor() {
        INITIATOR_EXECUTOR.shutdown();
        ACCEPTOR_EXECUTOR.shutdown();
    }

    private static TransportSetup TRANSPORT_SETUP_INITIATOR;
    private static TransportSetup TRANSPORT_SETUP_ACCEPTOR;

    protected static ServerEndpointConfig serverEndpointConfig() {
        return ServerEndpointConfig.Builder
            .create(Endpoint.class, PATH)
            .configurator(new WsConfigurator(AsyncWsConnection.factory(100), TRANSPORT_SETUP_ACCEPTOR, Exceptions.STD_ERR))
            .build();
    }

    protected static void setTransportSetup(final boolean serverInvoke, final boolean serverCreateException, final boolean clientInvoke, final boolean clientCreateException) {
        TRANSPORT_SETUP_ACCEPTOR = invokeTransportSetup(serverInvoke, serverCreateException, ACCEPTOR_EXECUTOR);
        TRANSPORT_SETUP_INITIATOR = invokeTransportSetup(clientInvoke, clientCreateException, INITIATOR_EXECUTOR);
    }

    protected static void setPerformanceSetup(final CountDownLatch latch) {
        TRANSPORT_SETUP_ACCEPTOR = performanceTransportSetup(ACCEPTOR_EXECUTOR);
        TRANSPORT_SETUP_INITIATOR = performanceTransportSetup(INITIATOR_EXECUTOR, latch, 100, 16);
    }

    protected static void connect(final WebSocketContainer container, final CountDownLatch latch) throws Exception {
        container.connectToServer(
            new WsConfigurator(SyncWsConnection.FACTORY, TRANSPORT_SETUP_INITIATOR, Exceptions.STD_ERR).getEndpointInstance(),
            ClientEndpointConfig.Builder.create().build(),
            THE_URI
        );
        latch.await();
        TimeUnit.MILLISECONDS.sleep(400L);
    }

}
