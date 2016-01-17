package ch.softappeal.yass.tutorial.acceptor.socket;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoServiceImpl;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.UnexpectedExceptionHandler;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

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
                ACCEPTOR.echoService.service(EchoServiceImpl.INSTANCE, UnexpectedExceptionHandler.INSTANCE, new Logger(null, Logger.Side.SERVER))
            )
        ).start(executor, SocketAcceptor.ADDRESS);
        System.out.println("started");
    }

}
