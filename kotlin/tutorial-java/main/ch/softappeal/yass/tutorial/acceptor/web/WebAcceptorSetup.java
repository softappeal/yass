package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.transport.SessionTransport;
import ch.softappeal.yass.transport.ws.WsConfigurator;
import ch.softappeal.yass.tutorial.acceptor.AcceptorSession;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.shared.web.WebSetup;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;

import static ch.softappeal.yass.ThreadFactoryKt.getStdErr;
import static ch.softappeal.yass.transport.ws.WsTransportKt.AsyncWsConnectionFactory;

public abstract class WebAcceptorSetup extends WebSetup {

    public static final ServerEndpointConfig ENDPOINT_CONFIG = ServerEndpointConfig.Builder
        .create(Endpoint.class, WS_PATH)
        .configurator(new WsConfigurator(
            AsyncWsConnectionFactory(1_000),
            new SessionTransport(
                Config.PACKET_SERIALIZER,
                () -> new AcceptorSession(DISPATCH_EXECUTOR)
            ),
            getStdErr()
        ))
        .build();

}
