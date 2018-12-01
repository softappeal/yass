package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.transport.*;
import ch.softappeal.yass.transport.ws.*;
import ch.softappeal.yass.tutorial.acceptor.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.shared.web.*;

import javax.websocket.*;
import javax.websocket.server.*;

import static ch.softappeal.yass.transport.ws.WsTransportKt.*;

public abstract class WebAcceptorSetup extends WebSetup {

    public static final ServerEndpointConfig ENDPOINT_CONFIG = ServerEndpointConfig.Builder
        .create(Endpoint.class, WS_PATH)
        .configurator(new WsConfigurator(
            asyncWsConnectionFactory(1_000),
            new SessionTransport(
                Config.PACKET_SERIALIZER,
                () -> new AcceptorSession(DISPATCH_EXECUTOR)
            )
        ))
        .build();

}
