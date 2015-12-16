package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.ws.AsyncWsConnection;
import ch.softappeal.yass.transport.ws.WsConnection;
import ch.softappeal.yass.transport.ws.WsEndpoint;
import ch.softappeal.yass.tutorial.acceptor.AcceptorSession;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import javax.websocket.Session;
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

    public static final class Endpoint extends WsEndpoint {
        @Override protected WsConnection createConnection(final Session session) throws Exception {
            return WsConnection.create(
                AsyncWsConnection.factory(1_000),
                TransportSetup.ofContractSerializer(
                    Config.SERIALIZER,
                    new SessionFactory() {
                        @Override public ch.softappeal.yass.core.remote.session.Session create(final Connection connection) throws Exception {
                            return new AcceptorSession(connection, DISPATCH_EXECUTOR);
                        }
                    }
                ),
                session
            );
        }
    }

    protected static ServerEndpointConfig endpointConfig(final ServerEndpointConfig.Configurator configurator) {
        return ServerEndpointConfig.Builder.create(Endpoint.class, PATH).configurator(configurator).build();
    }

}
