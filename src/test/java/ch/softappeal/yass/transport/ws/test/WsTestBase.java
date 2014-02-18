package ch.softappeal.yass.transport.ws.test;

import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.util.Exceptions;

import javax.websocket.Session;
import java.net.URI;
import java.util.concurrent.ExecutorService;

public abstract class WsTestBase {

  protected static volatile SessionSetup SESSION_SETUP_CLIENT;
  protected static volatile SessionSetup SESSION_SETUP_SERVER;

  protected static volatile Serializer PACKET_SERIALIZER;

  protected static volatile ExecutorService REQUEST_EXECUTOR;

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

  protected static final int PORT = 9090;
  protected static final String PATH = "/test";
  protected static final URI THE_URI = URI.create("ws://localhost:" + PORT + PATH);

}
