package ch.softappeal.yass.tutorial.acceptor.socket;

import ch.softappeal.yass.transport.*;
import ch.softappeal.yass.tutorial.acceptor.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.shared.socket.*;

import java.util.concurrent.*;

import static ch.softappeal.yass.ThreadFactoryKt.*;
import static ch.softappeal.yass.transport.socket.SessionSocketTransportKt.*;
import static ch.softappeal.yass.transport.socket.SocketKt.*;

public final class SocketAcceptor {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(namedThreadFactory("executor", getStdErr()));
        socketAcceptor(
            new AcceptorSetup(Config.PACKET_SERIALIZER, () -> new AcceptorSession(executor)),
            executor,
            asyncSocketConnectionFactory(executor, 1_000)
        ).start(executor, socketBinder(SocketSetup.ADDRESS));
        System.out.println("started");
    }

}
