package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.core.remote.session.test.LocalConnectionTest;
import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.socket.test.SocketPerformanceTest;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.util.Exceptions;
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

  private static volatile SessionSetup SESSION_SETUP_CLIENT;
  private static volatile SessionSetup SESSION_SETUP_SERVER;
  private static volatile Serializer PACKET_SERIALIZER;

  public static final class ClientEndpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) {
      return new WsConnection(SESSION_SETUP_CLIENT, PACKET_SERIALIZER, Exceptions.STD_ERR, session);
    }
  }

  public static final class ServerEndpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) {
      return new WsConnection(SESSION_SETUP_SERVER, PACKET_SERIALIZER, Exceptions.STD_ERR, session);
    }
  }

  protected static void setTransportSetup(
    final boolean serverInvoke, final boolean serverCreateException, final boolean serverOpenedException, final boolean serverInvokeBeforeOpened,
    final boolean clientInvoke, final boolean clientCreateException, final boolean clientOpenedException, final boolean clientInvokeBeforeOpened
  ) {
    PACKET_SERIALIZER = PacketSerializerTest.SERIALIZER;
    SESSION_SETUP_SERVER = LocalConnectionTest.createSetup(serverInvoke, "server", REQUEST_EXECUTOR, serverCreateException, serverOpenedException, serverInvokeBeforeOpened);
    SESSION_SETUP_CLIENT = LocalConnectionTest.createSetup(clientInvoke, "client", REQUEST_EXECUTOR, clientCreateException, clientOpenedException, clientInvokeBeforeOpened);
  }

  protected static void setPerformanceSetup(final CountDownLatch latch) {
    PACKET_SERIALIZER = SocketPerformanceTest.PACKET_SERIALIZER;
    SESSION_SETUP_SERVER = PerformanceTest.createSetup(REQUEST_EXECUTOR, null, SocketPerformanceTest.COUNTER);
    SESSION_SETUP_CLIENT = PerformanceTest.createSetup(REQUEST_EXECUTOR, latch, SocketPerformanceTest.COUNTER);
  }

  protected static void connect(final WebSocketContainer container, final CountDownLatch latch) throws Exception {
    final ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
    container.connectToServer(new ClientEndpoint(), config, THE_URI);
    latch.await();
    TimeUnit.MILLISECONDS.sleep(400L);
  }

}
