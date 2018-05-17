package ch.softappeal.yass.tutorial.initiator.web;

import ch.softappeal.yass.transport.SessionTransport;
import ch.softappeal.yass.transport.ws.WsConfigurator;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.initiator.InitiatorSession;
import ch.softappeal.yass.tutorial.shared.web.WebSetup;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.WebSocketContainer;
import java.net.URI;

import static ch.softappeal.yass.Kt.getStdErr;
import static ch.softappeal.yass.transport.ws.Kt.getSyncWsConnectionFactory;

public abstract class WebInitiatorSetup extends WebSetup {

    protected static void run(final WebSocketContainer container) throws Exception {
        container.connectToServer(
            new WsConfigurator(
                getSyncWsConnectionFactory(),
                new SessionTransport(
                    Config.PACKET_SERIALIZER,
                    () -> new InitiatorSession(DISPATCH_EXECUTOR)
                ),
                getStdErr()
            ).getEndpointInstance(),
            ClientEndpointConfig.Builder.create().build(),
            URI.create("ws://" + HOST + ":" + PORT + WS_PATH)
        );
        System.out.println("started");
    }

}
