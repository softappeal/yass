package ch.softappeal.yass.tutorial.initiator.web;

import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.ws.SyncWsConnection;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.tutorial.acceptor.web.WebAcceptorSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.initiator.InitiatorSession;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;

public abstract class WebInitiatorSetup {

    private static final class Endpoint extends WsEndpoint {
        @Override protected WsConnection createConnection(final Session session) throws Exception {
            return WsConnection.create(
                SyncWsConnection.FACTORY,
                TransportSetup.ofContractSerializer(
                    Config.SERIALIZER,
                    connection -> new InitiatorSession(connection, WebAcceptorSetup.DISPATCH_EXECUTOR)
                ),
                session
            );
        }
    }

    protected static void run(final WebSocketContainer container) throws Exception {
        container.connectToServer(
            new Endpoint(),
            ClientEndpointConfig.Builder.create().build(),
            URI.create("ws://" + WebAcceptorSetup.HOST + ":" + WebAcceptorSetup.PORT + WebAcceptorSetup.PATH)
        );
        System.out.println("started");
    }

}
