package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.ws.AsyncWsConnection;
import ch.softappeal.yass.transport.ws.WsConfigurator;
import ch.softappeal.yass.tutorial.acceptor.AcceptorSession;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class WebAcceptorSetup {

    public static final String HOST = "0.0.0.0";
    public static final int PORT = 9090;
    public static final String PATH = "/tutorial";
    protected static final String XHR_PATH = "/xhr";
    protected static final String WEB_PATH = ".";

    public static final Executor DISPATCH_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("dispatchExecutor", Exceptions.STD_ERR));

    protected static final ServerEndpointConfig ENDPOINT_CONFIG = ServerEndpointConfig.Builder
        .create(Endpoint.class, PATH)
        .configurator(
            new WsConfigurator(
                AsyncWsConnection.factory(1_000),
                TransportSetup.ofContractSerializer(
                    Config.CONTRACT_SERIALIZER,
                    connection -> new AcceptorSession(connection, DISPATCH_EXECUTOR)
                ),
                Exceptions.STD_ERR
            )
        ).build();

}
