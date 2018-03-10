package ch.softappeal.yass.tutorial.initiator.socket;

import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.socket.SocketConnector;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.socket.SocketSetup;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public final class SimpleSocketInitiator {

    public static void main(final String... args) {
        final var client = SimpleSocketTransport.client(Config.MESSAGE_SERIALIZER, SocketConnector.create(SocketSetup.ADDRESS));
        final var echoService = client.proxy(ACCEPTOR.echoService, new Logger(null, Logger.Side.CLIENT));
        System.out.println(echoService.echo("echo"));
    }

}
