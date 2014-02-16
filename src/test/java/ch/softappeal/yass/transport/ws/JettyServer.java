package ch.softappeal.yass.transport.ws;

import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.PacketSerializer;
import ch.softappeal.yass.tutorial.contract.Price;
import ch.softappeal.yass.tutorial.contract.PriceType;
import ch.softappeal.yass.tutorial.contract.Trade;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.tutorial.contract.instrument.Bond;
import ch.softappeal.yass.tutorial.contract.instrument.Stock;
import ch.softappeal.yass.tutorial.server.ServerMain;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.Executors;

public final class JettyServer {

  private static final int PORT = 9090;
  private static final String PATH = "/tutorial";
  public static final URI THE_URI = URI.create("ws://localhost:" + PORT + PATH);

  public static final JsSerializer CONTRACT_SERIALIZER = new JsSerializer(
    FastReflector.FACTORY,
    Arrays.<Class<?>>asList(PriceType.class),
    Arrays.<Class<?>>asList(Price.class, Trade.class, UnknownInstrumentsException.class),
    Arrays.<Class<?>>asList(Stock.class, Bond.class)
  );

  public static final Serializer PACKET_SERIALIZER = new PacketSerializer(new MessageSerializer(CONTRACT_SERIALIZER));

  private static final SessionSetup SESSION_SETUP = ServerMain.createSessionSetup(
    Executors.newCachedThreadPool(new NamedThreadFactory("requestExecutor", Exceptions.STD_ERR))
  );

  public static final class Endpoint extends WsEndpoint {
    @Override protected WsConnection createConnection(final Session session) {
      return new WsConnection(SESSION_SETUP, PACKET_SERIALIZER, Exceptions.STD_ERR, session);
    }
  }

  public static void main(final String... args) throws Exception {
    final Server server = new Server(PORT);
    server.addConnector(new ServerConnector(server));
    final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    WebSocketServerContainerInitializer.configureContext(context).addEndpoint(
      ServerEndpointConfig.Builder.create(Endpoint.class, PATH).build()
    );
    server.start();
  }

}
