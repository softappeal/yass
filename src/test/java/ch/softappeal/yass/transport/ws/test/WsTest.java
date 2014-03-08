package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.core.remote.session.test.LocalConnectionTest;
import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.test.SocketPerformanceTest;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
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

  @Before
  public void startRequestExecutor() {
    REQUEST_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("requestExecutor", TestUtils.TERMINATE));
  }

  @After
  public void stopRequestExecutor() {
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
    final boolean serverInvoke, final boolean serverCreateException, final boolean serverOpenedException, final boolean serverInvokeBeforeOpened,
    final boolean clientInvoke, final boolean clientCreateException, final boolean clientOpenedException, final boolean clientInvokeBeforeOpened
  ) {
    TRANSPORT_SETUP_SERVER = new TransportSetup(LocalConnectionTest.createSetup(serverInvoke, "server", REQUEST_EXECUTOR, serverCreateException, serverOpenedException, serverInvokeBeforeOpened), PacketSerializerTest.SERIALIZER);
    TRANSPORT_SETUP_CLIENT = new TransportSetup(LocalConnectionTest.createSetup(clientInvoke, "client", REQUEST_EXECUTOR, clientCreateException, clientOpenedException, clientInvokeBeforeOpened), PacketSerializerTest.SERIALIZER);
  }

  protected static void setPerformanceSetup(final CountDownLatch latch) {
    TRANSPORT_SETUP_SERVER = new TransportSetup(PerformanceTest.createSetup(REQUEST_EXECUTOR, null, SocketPerformanceTest.COUNTER), SocketPerformanceTest.PACKET_SERIALIZER);
    TRANSPORT_SETUP_CLIENT = new TransportSetup(PerformanceTest.createSetup(REQUEST_EXECUTOR, latch, SocketPerformanceTest.COUNTER), SocketPerformanceTest.PACKET_SERIALIZER);
  }

  protected static void connect(final WebSocketContainer container, final CountDownLatch latch) throws Exception {
    final ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
    container.connectToServer(new ClientEndpoint(), config, THE_URI);
    latch.await();
    TimeUnit.MILLISECONDS.sleep(400L);
  }

}
