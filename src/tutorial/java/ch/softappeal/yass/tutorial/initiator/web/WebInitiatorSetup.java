package ch.softappeal.yass.tutorial.initiator.web;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.ws.SyncWsConnection;
import ch.softappeal.yass.transport.ws.WsConfigurator;
import ch.softappeal.yass.tutorial.acceptor.web.WebAcceptorSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.initiator.InitiatorSession;
import ch.softappeal.yass.util.Exceptions;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.WebSocketContainer;
import java.net.URI;

public abstract class WebInitiatorSetup {

    protected static void run(final WebSocketContainer container) throws Exception {
        container.connectToServer(
            new WsConfigurator(
                SyncWsConnection.FACTORY,
                TransportSetup.ofContractSerializer(
                    Config.CONTRACT_SERIALIZER,
                    new SessionFactory() {
                        @Override public Session create(final Connection connection) throws Exception {
                            return new InitiatorSession(connection, WebAcceptorSetup.DISPATCH_EXECUTOR);
                        }
                    }
                ),
                Exceptions.STD_ERR
            ).getEndpointInstance(),
            ClientEndpointConfig.Builder.create().build(),
            URI.create("ws://" + WebAcceptorSetup.HOST + ":" + WebAcceptorSetup.PORT + WebAcceptorSetup.PATH)
        );
        System.out.println("started");
    }

}
