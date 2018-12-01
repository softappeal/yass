package ch.softappeal.yass.tutorial.acceptor.socket;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.transport.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.shared.*;
import ch.softappeal.yass.tutorial.shared.socket.*;

import java.util.concurrent.*;

import static ch.softappeal.yass.ThreadFactoryKt.*;
import static ch.softappeal.yass.remote.ServerKt.*;
import static ch.softappeal.yass.transport.socket.SocketKt.*;
import static ch.softappeal.yass.transport.socket.SocketTransportKt.*;
import static ch.softappeal.yass.tutorial.contract.Config.*;

public final class SocketServer {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(namedThreadFactory("executor", getStdErr()));
        socketServer(
            new ServerSetup(
                new Server(
                    service(
                        ACCEPTOR.echoService,
                        EchoServiceImpl.INSTANCE,
                        UnexpectedExceptionHandler.INSTANCE, new Logger(null, Logger.Side.SERVER)
                    )
                ),
                Config.MESSAGE_SERIALIZER
            ),
            executor
        ).start(executor, socketBinder(SocketSetup.ADDRESS));
        System.out.println("started");
    }

}
