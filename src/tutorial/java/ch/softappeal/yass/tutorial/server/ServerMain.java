package ch.softappeal.yass.tutorial.server;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.ServerServices;
import ch.softappeal.yass.util.ContextLocator;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ServerMain {

  public static SessionSetup createSessionSetup(final Executor requestExecutor) {
    return new SessionSetup(
      new Server(
        Config.METHOD_MAPPER_FACTORY,
        ServerServices.InstrumentService.service(new InstrumentServiceImpl(), Logger.SERVER),
        ServerServices.PriceEngine.service(
          new PriceEngineImpl(
            new ContextLocator<PriceEngineContext>() {
              @Override public PriceEngineContext context() {
                return (PriceEngineContext)Session.get();
              }
            }
          ),
          Logger.SERVER
        )
      ),
      requestExecutor,
      new SessionFactory() {
        @Override public Session create(final SessionSetup setup, final Connection connection) {
          return new ServerSession(setup, connection);
        }
      }
    );
  }

  public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

  public static void main(final String... args) {
    final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    new SocketTransport(
      createSessionSetup(executor), Config.PACKET_SERIALIZER, executor, executor, Exceptions.STD_ERR
    ).start(executor, ADDRESS);
    System.out.println("started");
  }

}
