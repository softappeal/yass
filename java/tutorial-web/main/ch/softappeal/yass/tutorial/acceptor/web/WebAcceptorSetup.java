package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.ws.AsyncWsConnection;
import ch.softappeal.yass.transport.ws.WsConfigurator;
import ch.softappeal.yass.tutorial.acceptor.AcceptorSession;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.shared.web.WebSetup;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;

public abstract class WebAcceptorSetup extends WebSetup {

    public static final ServerEndpointConfig ENDPOINT_CONFIG = ServerEndpointConfig.Builder
        .create(Endpoint.class, WS_PATH)
        .configurator(new WsConfigurator(
            AsyncWsConnection.factory(1_000),
            TransportSetup.ofContractSerializer(
                Config.CONTRACT_SERIALIZER,
                connection -> new AcceptorSession(connection, DISPATCH_EXECUTOR)
            ),
            Exceptions.STD_ERR
        )).build();

}
