package ch.softappeal.yass.tutorial.initiator.web;

import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.ws.SyncWsConnection;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.tutorial.acceptor.web.WsAcceptorSetup;
import ch.softappeal.yass.tutorial.initiator.InitiatorSession;
import ch.softappeal.yass.tutorial.initiator.InitiatorSetup;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;

public abstract class WsInitiatorSetup extends InitiatorSetup {

    private static final TransportSetup TRANSPORT_SETUP = createTransportSetup(connection -> new InitiatorSession(connection, WsAcceptorSetup.DISPATCH_EXECUTOR));

    private static final class Endpoint extends WsEndpoint {
        @Override protected WsConnection createConnection(final Session session) throws Exception {
            return WsConnection.create(SyncWsConnection.FACTORY, TRANSPORT_SETUP, session);
        }
    }

    protected static void run(final WebSocketContainer container) throws Exception {
        container.connectToServer(
            new Endpoint(),
            ClientEndpointConfig.Builder.create().build(),
            URI.create("ws://" + WsAcceptorSetup.HOST + ":" + WsAcceptorSetup.PORT + WsAcceptorSetup.PATH)
        );
        System.out.println("started");
    }

}
