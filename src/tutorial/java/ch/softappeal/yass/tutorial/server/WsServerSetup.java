package ch.softappeal.yass.tutorial.server;

import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class WsServerSetup extends ServerSetup {

  public static final String HOST = "localhost";
  public static final int PORT = 9090;
  public static final String PATH = "/tutorial";

  public static final Executor REQUEST_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("requestExecutor", Exceptions.STD_ERR));

  private static final TransportSetup TRANSPORT_SETUP = SocketServer.createTransportSetup(REQUEST_EXECUTOR);

  public static final class Endpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) throws Exception {
      return new WsConnection(TRANSPORT_SETUP, session);
    }
  }

  protected static final ServerEndpointConfig ENDPOINT_CONFIG = ServerEndpointConfig.Builder.create(Endpoint.class, PATH).build();

}
