package ch.softappeal.yass.tutorial.acceptor.socket;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.NamedThreadFactory;
import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.remote.Service;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.socket.SocketBinder;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.shared.EchoServiceImpl;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.UnexpectedExceptionHandler;
import ch.softappeal.yass.tutorial.shared.socket.SocketSetup;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public final class SimpleSocketAcceptor {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        new SimpleSocketTransport(
            executor,
            Config.MESSAGE_SERIALIZER,
            new Server(
                new Service(ACCEPTOR.echoService, EchoServiceImpl.INSTANCE, UnexpectedExceptionHandler.INSTANCE, new Logger(null, Logger.Side.SERVER))
            )
        ).start(executor, SocketBinder.create(SocketSetup.ADDRESS));
        System.out.println("started");
    }

}
