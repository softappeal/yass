package ch.softappeal.yass.tutorial.server;

import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.socket.SocketExecutor;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class SocketServer extends ServerSetup {

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
