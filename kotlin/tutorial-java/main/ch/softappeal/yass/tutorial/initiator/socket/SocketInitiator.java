package ch.softappeal.yass.tutorial.initiator.socket;

import ch.softappeal.yass.transport.InitiatorSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.initiator.InitiatorSession;
import ch.softappeal.yass.tutorial.shared.socket.SocketSetup;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.ThreadFactoryKt.getStdErr;
import static ch.softappeal.yass.ThreadFactoryKt.namedThreadFactory;
import static ch.softappeal.yass.transport.socket.SessionSocketTransportKt.getSyncSocketConnectionFactory;
import static ch.softappeal.yass.transport.socket.SessionSocketTransportKt.socketInitiate;
import static ch.softappeal.yass.transport.socket.SocketKt.socketConnector;

public final class SocketInitiator {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(namedThreadFactory("executor", getStdErr()));
        socketInitiate(
            new InitiatorSetup(Config.PACKET_SERIALIZER, () -> new InitiatorSession(executor)),
            executor,
            getSyncSocketConnectionFactory(),
            socketConnector(SocketSetup.ADDRESS)
        );
        System.out.println("started");
    }

}
