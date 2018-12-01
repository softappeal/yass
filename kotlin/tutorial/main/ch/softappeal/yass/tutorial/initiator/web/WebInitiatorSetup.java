package ch.softappeal.yass.tutorial.initiator.web;

import ch.softappeal.yass.transport.*;
import ch.softappeal.yass.transport.ws.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.initiator.*;
import ch.softappeal.yass.tutorial.shared.web.*;

import javax.websocket.*;
import java.net.*;

import static ch.softappeal.yass.transport.ws.WsTransportKt.*;

public abstract class WebInitiatorSetup extends WebSetup {

    protected static void run(final WebSocketContainer container) throws Exception {
        container.connectToServer(
            new WsConfigurator(
                getSyncWsConnectionFactory(),
                new SessionTransport(
                    Config.PACKET_SERIALIZER,
                    () -> new InitiatorSession(DISPATCH_EXECUTOR)
                )
            ).getEndpointInstance(),
            ClientEndpointConfig.Builder.create().build(),
            URI.create("ws://" + HOST + ":" + PORT + WS_PATH)
        );
        System.out.println("started");
    }

}
