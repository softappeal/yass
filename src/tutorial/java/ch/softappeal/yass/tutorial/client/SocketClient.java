package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.core.remote.session.Reconnector;
import ch.softappeal.yass.transport.socket.SocketExecutor;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.server.SocketServer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class SocketClient extends ClientSetup {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        Reconnector.start(executor, 5, ClientSession::new, sessionFactory -> {
            try {
                SocketTransport.connect(
                    createTransportSetup(executor, sessionFactory),
                    new SocketExecutor(executor, Exceptions.STD_ERR),
                    Config.PATH_SERIALIZER, SocketServer.PATH,
                    SocketServer.ADDRESS
                );
            } catch (final RuntimeException e) {
                System.out.println("connect failed: " + e.getMessage());
            }
        });
        System.out.println("started");
    }

}
