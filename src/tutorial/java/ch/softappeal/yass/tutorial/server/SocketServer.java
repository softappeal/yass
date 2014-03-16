package ch.softappeal.yass.tutorial.server;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketExecutor;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.ServerServices;
import ch.softappeal.yass.util.ContextLocator;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class SocketServer {

  private static final PriceEngine PRICE_ENGINE = new PriceEngineImpl(new ContextLocator<PriceEngineContext>() {
    @Override public PriceEngineContext context() {
      return (PriceEngineContext)Session.get();
    }
  });

  private static final Server SERVER = new Server(
    Config.METHOD_MAPPER_FACTORY,
    ServerServices.InstrumentService.service(new InstrumentServiceImpl(), Logger.SERVER),
    ServerServices.PriceEngine.service(PRICE_ENGINE, Logger.SERVER)
  );

  public static TransportSetup createTransportSetup(final Executor requestExecutor) {
    return new TransportSetup(SERVER, requestExecutor, Config.PACKET_SERIALIZER) {
      @Override public Session createSession(final SessionClient sessionClient) {
        return new ServerSession(sessionClient);
      }
    };
  }

  public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);
  public static final String PATH = "tutorial";

  public static void main(final String... args) {
    final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    SocketTransport.listener(
      Config.PATH_SERIALIZER, new PathResolver(PATH, createTransportSetup(executor))
    ).start(executor, new SocketExecutor(executor, Exceptions.STD_ERR), ADDRESS);
    System.out.println("started");
  }

}
