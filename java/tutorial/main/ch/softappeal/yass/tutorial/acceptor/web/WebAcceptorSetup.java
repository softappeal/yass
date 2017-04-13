package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.ws.AsyncWsConnection;
import ch.softappeal.yass.transport.ws.WsConfigurator;
import ch.softappeal.yass.tutorial.acceptor.AcceptorSession;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import javax.websocket.Endpoint;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class WebAcceptorSetup {

    public static final String HOST = "0.0.0.0";
    public static final int PORT = 9090;
    public static final String WS_PATH = "/ws";
    public static final String XHR_PATH = "/xhr";
    protected static final String WEB_PATH = "ts";

    public static final Executor DISPATCH_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("dispatchExecutor", Exceptions.STD_ERR));

    public static final ServerEndpointConfig ENDPOINT_CONFIG = ServerEndpointConfig.Builder
        .create(Endpoint.class, WS_PATH)
        .configurator(
            new WsConfigurator(
                AsyncWsConnection.factory(1_000),
                TransportSetup.ofContractSerializer(
                    Config.CONTRACT_SERIALIZER,
                    connection -> new AcceptorSession(connection, DISPATCH_EXECUTOR)
                ),
                Exceptions.STD_ERR
            ) {
                @Override public void modifyHandshake(final ServerEndpointConfig sec, final HandshakeRequest request, final HandshakeResponse response) {
                    // see:
                    //     http://dev.eclipse.org/mhonarc/lists/jetty-users/msg07615.html
                    //     http://lists.jboss.org/pipermail/undertow-dev/2017-February/001892.html
                    //     https://java.net/jira/browse/WEBSOCKET_SPEC-218
                    //     https://java.net/jira/browse/WEBSOCKET_SPEC-235
                    //     http://stackoverflow.com/questions/17936440/accessing-httpsession-from-httpservletrequest-in-a-web-socket-serverendpoint/17994303#17994303
                    sec.getUserProperties().putAll(request.getHeaders());
                }
            }
        ).build();

}
