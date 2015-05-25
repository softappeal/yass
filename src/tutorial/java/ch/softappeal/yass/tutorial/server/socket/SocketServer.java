package ch.softappeal.yass.tutorial.server.socket;

import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.tutorial.server.ServerSetup;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class SocketServer extends ServerSetup {

    public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        SocketTransport.listener(createTransportSetup(executor)).start(executor, executor, ADDRESS);
        System.out.println("started");
    }

}
