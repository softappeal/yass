package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.core.remote.session.test.LocalConnectionTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.util.Exceptions;

import javax.websocket.Session;
import java.net.URI;
import java.util.concurrent.ExecutorService;

public abstract class WsTransportTest extends InvokeTest { // $todo: review

  private static volatile SessionSetup SESSION_SETUP_CLIENT;
  private static volatile SessionSetup SESSION_SETUP_SERVER;

  protected static volatile ExecutorService REQUEST_EXECUTOR;

  private static SessionSetup createSetup(
    final boolean invoke, final String name, final boolean createException, final boolean openedException, final boolean invokeBeforeOpened
  ) {
    return LocalConnectionTest.createSetup(
      invoke, name, REQUEST_EXECUTOR, createException, openedException, invokeBeforeOpened
    );
  }

  protected static void createSetup(
    final boolean serverInvoke, final boolean serverCreateException, final boolean serverOpenedException, final boolean serverInvokeBeforeOpened,
    final boolean clientInvoke, final boolean clientCreateException, final boolean clientOpenedException, final boolean clientInvokeBeforeOpened
  ) {
    SESSION_SETUP_SERVER = createSetup(serverInvoke, "server", serverCreateException, serverOpenedException, serverInvokeBeforeOpened);
    SESSION_SETUP_CLIENT = createSetup(clientInvoke, "client", clientCreateException, clientOpenedException, clientInvokeBeforeOpened);
  }

  public static final class ClientEndpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) {
      return new WsConnection(SESSION_SETUP_CLIENT, PacketSerializerTest.SERIALIZER, Exceptions.STD_ERR, session);
    }
  }

  public static final class ServerEndpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) {
      return new WsConnection(SESSION_SETUP_SERVER, PacketSerializerTest.SERIALIZER, Exceptions.STD_ERR, session);
    }
  }

  protected static final int PORT = 9090;
  protected static final String PATH = "/test";
  protected static final URI THE_URI = URI.create("ws://localhost:" + PORT + PATH);

}
