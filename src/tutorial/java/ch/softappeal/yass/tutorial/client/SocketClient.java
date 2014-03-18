package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketExecutor;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.tutorial.contract.ClientServices;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.PriceListener;
import ch.softappeal.yass.tutorial.server.SocketServer;
import ch.softappeal.yass.util.ContextLocator;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class SocketClient {

  private static final PriceListener PRICE_LISTENER = new PriceListenerImpl(new ContextLocator<PriceListenerContext>() {
    @Override public PriceListenerContext context() {
      return (PriceListenerContext)Session.get();
    }
  });

  public static final Server SERVER = new Server(
    Config.METHOD_MAPPER_FACTORY,
    new Service(ClientServices.PriceListener, PRICE_LISTENER)
  );

  public static TransportSetup createTransportSetup(final Executor requestExecutor) {
    return new TransportSetup(SERVER, requestExecutor, Config.PACKET_SERIALIZER) {
      @Override public Session createSession(final SessionClient sessionClient) {
        return new ClientSession(sessionClient);
      }
    };
  }

  public static void main(final String... args) {
    final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    SocketTransport.connect(
      createTransportSetup(executor), new SocketExecutor(executor, Exceptions.STD_ERR),
      Config.PATH_SERIALIZER, SocketServer.PATH,
      SocketServer.ADDRESS
    );
    System.out.println("started");
  }

}
