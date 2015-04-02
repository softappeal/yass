package ch.softappeal.yass.tutorial.client.socket;

import ch.softappeal.yass.transport.socket.SocketExecutor;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.tutorial.client.ClientSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.server.socket.SocketServer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class SocketClient extends ClientSetup {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        SocketTransport.connect(
            createTransportSetup(executor),
            new SocketExecutor(executor, Exceptions.STD_ERR),
            Config.PATH_SERIALIZER, SocketServer.PATH,
            SocketServer.ADDRESS
        );
        System.out.println("started");
    }

}
