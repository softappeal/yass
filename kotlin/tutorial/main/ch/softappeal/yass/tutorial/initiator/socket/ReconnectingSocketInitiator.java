package ch.softappeal.yass.tutorial.initiator.socket;

import ch.softappeal.yass.transport.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.initiator.*;
import ch.softappeal.yass.tutorial.shared.socket.*;

import java.util.concurrent.*;

import static ch.softappeal.yass.ThreadFactoryKt.*;
import static ch.softappeal.yass.transport.socket.SessionSocketTransportKt.*;
import static ch.softappeal.yass.transport.socket.SocketKt.*;

public final class ReconnectingSocketInitiator {

    public static void main(final String... args) throws InterruptedException {
        final Executor executor = Executors.newCachedThreadPool(namedThreadFactory("executor", getStdErr()));
        final InitiatorReconnector reconnector = new InitiatorReconnector();
        reconnector.start(
            executor,
            10,
            () -> new InitiatorSession(executor),
            sessionFactory -> {
                socketInitiator(
                    new InitiatorSetup(Config.PACKET_SERIALIZER, sessionFactory),
                    executor,
                    getSyncSocketConnectionFactory(),
                    socketConnector(SocketSetup.ADDRESS)
                );
                return null;
            }
        );
        System.out.println("started");
        while (true) {
            TimeUnit.SECONDS.sleep(1L);
            if (reconnector.isConnected()) {
                try {
                    System.out.println(reconnector.echoService.echo("knock"));
                } catch (final Exception e) {
                    System.out.println("race condition: " + e);
                }
            } else {
                System.out.println("not connected");
            }
        }
    }

}
