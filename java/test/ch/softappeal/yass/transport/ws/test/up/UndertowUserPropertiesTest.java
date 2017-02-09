package ch.softappeal.yass.transport.ws.test.up;

import io.undertow.Undertow;
import io.undertow.server.XnioByteBufferPool;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.core.ContextClassLoaderSetupAction;
import io.undertow.servlet.util.DefaultClassIntrospector;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;

public class UndertowUserPropertiesTest extends UserPropertiesTest {

    public static void main(String... args) throws Exception {
        Xnio xnio = Xnio.getInstance();
        DeploymentManager deployment = Servlets.defaultContainer()
            .addDeployment(
                Servlets.deployment()
                    .setClassLoader(UndertowUserPropertiesTest.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName(UndertowUserPropertiesTest.class.getName())
                    .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                            .addEndpoint(ServerEndpointConfig.Builder.create(Endpoint.class, PATH).configurator(SERVER_CONFIGURATOR).build())
                            .setWorker(xnio.createWorker(OptionMap.builder().getMap()))
                            .setBuffers(new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)))
                    )
            );
        deployment.deploy();
        Undertow.builder().addHttpListener(PORT, "localhost").setHandler(deployment.start()).build().start();
        clientConnect(new ServerWebSocketContainer(
            DefaultClassIntrospector.INSTANCE,
            xnio.createWorker(OptionMap.create(Options.THREAD_DAEMON, true)),
            new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)),
            Collections.singletonList(new ContextClassLoaderSetupAction(ClassLoader.getSystemClassLoader())),
            true,
            true
        ));
    }

}
