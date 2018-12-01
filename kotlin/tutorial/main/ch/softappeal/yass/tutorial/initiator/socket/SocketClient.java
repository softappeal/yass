package ch.softappeal.yass.tutorial.initiator.socket;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.transport.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.shared.*;
import ch.softappeal.yass.tutorial.shared.socket.*;

import static ch.softappeal.yass.transport.socket.SocketKt.*;
import static ch.softappeal.yass.transport.socket.SocketTransportKt.*;
import static ch.softappeal.yass.tutorial.contract.Config.*;

public final class SocketClient {

    public static void main(final String... args) {
        final Client client =
            socketClient(new ClientSetup(Config.MESSAGE_SERIALIZER), socketConnector(SocketSetup.ADDRESS));
        final EchoService echoService = client.proxy(ACCEPTOR.echoService, new Logger(null, Logger.Side.CLIENT));
        System.out.println(echoService.echo("echo"));
    }

}
