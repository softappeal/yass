package ch.softappeal.yass.tutorial.initiator.socket;

import ch.softappeal.yass.transport.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.initiator.*;
import ch.softappeal.yass.tutorial.shared.socket.*;

import java.util.concurrent.*;

import static ch.softappeal.yass.ThreadFactoryKt.*;
import static ch.softappeal.yass.transport.socket.SessionSocketTransportKt.*;
import static ch.softappeal.yass.transport.socket.SocketKt.*;

public final class SocketInitiator {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(namedThreadFactory("executor", getStdErr()));
        socketInitiator(
            new InitiatorSetup(Config.PACKET_SERIALIZER, () -> new InitiatorSession(executor)),
            executor,
            getSyncSocketConnectionFactory(),
            socketConnector(SocketSetup.ADDRESS)
        );
        System.out.println("started");
    }

}
