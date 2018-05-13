package ch.softappeal.yass.tutorial.acceptor.socket;

import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.transport.ServerSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.shared.EchoServiceImpl;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.UnexpectedExceptionHandler;
import ch.softappeal.yass.tutorial.shared.socket.SocketSetup;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.ThreadFactoryKt.getStdErr;
import static ch.softappeal.yass.ThreadFactoryKt.namedThreadFactory;
import static ch.softappeal.yass.remote.ServerKt.service;
import static ch.softappeal.yass.transport.socket.SocketKt.socketBinder;
import static ch.softappeal.yass.transport.socket.SocketTransportKt.socketServer;
import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public final class SocketServer {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(namedThreadFactory("executor", getStdErr()));
        socketServer(
            new ServerSetup(
                new Server(
                    service(ACCEPTOR.echoService, EchoServiceImpl.INSTANCE, UnexpectedExceptionHandler.INSTANCE, new Logger(null, Logger.Side.SERVER))
                ),
                Config.MESSAGE_SERIALIZER
            ),
            executor
        ).start(executor, socketBinder(SocketSetup.ADDRESS));
        System.out.println("started");
    }

}
