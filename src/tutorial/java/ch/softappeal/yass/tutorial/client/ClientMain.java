package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.tutorial.contract.ClientServices;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.server.ServerMain;
import ch.softappeal.yass.util.ContextLocator;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ClientMain {

  public static void main(final String... args) {
    final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    final SocketTransport transport = new SocketTransport(
      new SessionSetup(
        new Server(
          Config.METHOD_MAPPER_FACTORY,
          ClientServices.PriceListener.service(
            new PriceListenerImpl(
              new ContextLocator<PriceListenerContext>() {
                @Override public PriceListenerContext context() {
                  return (PriceListenerContext)Session.get();
                }
              }
            )
          )
        ),
        executor,
        new SessionFactory() {
          @Override public Session create(final SessionSetup setup, final Connection connection) {
            return new ClientSession(setup, connection);
          }
        }
      ),
      Config.PACKET_SERIALIZER,
      executor, executor,
      Exceptions.STD_ERR
    );
    transport.connect(ServerMain.ADDRESS); // connect to node 1
    transport.connect(ServerMain.ADDRESS); // connect to node 2 (simulated)
    System.out.println("started");
  }

}
