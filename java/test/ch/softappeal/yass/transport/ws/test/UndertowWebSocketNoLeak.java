package ch.softappeal.yass.transport.ws.test;

import io.undertow.Undertow;
import io.undertow.server.XnioByteBufferPool;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.core.CompositeThreadSetupAction;
import io.undertow.servlet.util.DefaultClassIntrospector;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;

public class UndertowWebSocketNoLeak {

    public static void main(String... args) throws Exception {
        Xnio xnio = Xnio.getInstance();
        DeploymentManager deployment = Servlets.defaultContainer()
            .addDeployment(
                Servlets.deployment()
                    .setClassLoader(UndertowTest.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName(UndertowTest.class.getName())
                    .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                            .addEndpoint(
                                ServerEndpointConfig.Builder
                                    .create(Endpoint.class, JettyWebSocketLeak.PATH)
                                    .configurator(new JettyWebSocketLeak.WsConfigurator("server"))
                                    .build()
                            )
                            .setWorker(xnio.createWorker(OptionMap.builder().getMap()))
                            .setBuffers(new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)))
                    )
            );
        deployment.deploy();
        Undertow server = Undertow.builder()
            .addHttpListener(JettyWebSocketLeak.PORT, "localhost")
            .setHandler(deployment.start())
            .build();
        server.start();
        new ServerWebSocketContainer(
            DefaultClassIntrospector.INSTANCE,
            xnio.createWorker(OptionMap.create(Options.THREAD_DAEMON, true)),
            new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)),
            new CompositeThreadSetupAction(Collections.emptyList()),
            true,
            true
        ).connectToServer(
            new JettyWebSocketLeak.WsConfigurator("client").getEndpointInstance(Endpoint.class),
            ClientEndpointConfig.Builder.create().build(),
            JettyWebSocketLeak.THE_URI
        );
        /* program output:

        opening server session 21347760
        opening client session 1154002927

        closing client session 1154002927
        client session 1154002927 closed with CloseReason[1000]
        server session 21347760 closed with CloseReason[1000]

        */
    }

}
