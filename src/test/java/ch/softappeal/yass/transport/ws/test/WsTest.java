package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.core.remote.session.test.LocalConnectionTest;
import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.test.SocketPerformanceTest;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.TestUtils;
import org.junit.After;
import org.junit.Before;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class WsTest {

  protected static final int PORT = 9090;
  protected static final String PATH = "/test";
  protected static final URI THE_URI = URI.create("ws://localhost:" + PORT + PATH);

  private static volatile ExecutorService REQUEST_EXECUTOR;

  @Before public void startRequestExecutor() {
    REQUEST_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("requestExecutor", TestUtils.TERMINATE));
  }

  @After public void stopRequestExecutor() {
    REQUEST_EXECUTOR.shutdown();
  }

  private static volatile TransportSetup TRANSPORT_SETUP_CLIENT;
  private static volatile TransportSetup TRANSPORT_SETUP_SERVER;

  public static final class ClientEndpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) throws Exception {
      return new WsConnection(TRANSPORT_SETUP_CLIENT, session);
    }
  }

  public static final class ServerEndpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) throws Exception {
      return new WsConnection(TRANSPORT_SETUP_SERVER, session);
    }
  }

  protected static void setTransportSetup(
    final boolean serverInvoke, final boolean serverCreateException,
    final boolean clientInvoke, final boolean clientCreateException
  ) {
    TRANSPORT_SETUP_SERVER = LocalConnectionTest.createSetup(serverInvoke, REQUEST_EXECUTOR, serverCreateException);
    TRANSPORT_SETUP_CLIENT = LocalConnectionTest.createSetup(clientInvoke, REQUEST_EXECUTOR, clientCreateException);
  }

  protected static void setPerformanceSetup(final CountDownLatch latch) {
    TRANSPORT_SETUP_SERVER = PerformanceTest.createSetup(REQUEST_EXECUTOR, null, SocketPerformanceTest.COUNTER);
    TRANSPORT_SETUP_CLIENT = PerformanceTest.createSetup(REQUEST_EXECUTOR, latch, SocketPerformanceTest.COUNTER);
  }

  protected static void connect(final WebSocketContainer container, final CountDownLatch latch) throws Exception {
    final ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
    container.connectToServer(new ClientEndpoint(), config, THE_URI);
    latch.await();
    TimeUnit.MILLISECONDS.sleep(400L);
  }

}
