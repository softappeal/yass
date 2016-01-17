package ch.softappeal.yass.tutorial.initiator.socket;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.tutorial.acceptor.socket.SocketAcceptor;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Logger;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public final class SimpleSocketInitiator {

    public static void main(final String... args) {
        final Client client = SimpleSocketTransport.client(Config.MESSAGE_SERIALIZER, SocketAcceptor.ADDRESS, 0, 0);
        final EchoService echoService = client.proxy(ACCEPTOR.echoService, new Logger(null, Logger.Side.CLIENT));
        System.out.println(echoService.echo("echo"));
    }

}
