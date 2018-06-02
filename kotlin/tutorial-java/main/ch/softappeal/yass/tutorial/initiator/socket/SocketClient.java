package ch.softappeal.yass.tutorial.initiator.socket;

import ch.softappeal.yass.remote.Client;
import ch.softappeal.yass.transport.ClientSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.socket.SocketSetup;

import static ch.softappeal.yass.transport.socket.Kt.socketClient;
import static ch.softappeal.yass.transport.socket.Kt.socketConnector;
import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public final class SocketClient {

    public static void main(final String... args) {
        final Client client = socketClient(new ClientSetup(Config.MESSAGE_SERIALIZER), socketConnector(SocketSetup.ADDRESS));
        final EchoService echoService = client.proxy(ACCEPTOR.echoService, new Logger(null, Logger.Side.CLIENT));
        System.out.println(echoService.echo("echo"));
    }

}
