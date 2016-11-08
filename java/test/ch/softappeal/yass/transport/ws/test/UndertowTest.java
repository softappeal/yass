package ch.softappeal.yass.transport.ws.test;

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

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public abstract class UndertowTest extends WsTest {

    protected static void run(final CountDownLatch latch) throws Exception {
        final Xnio xnio = Xnio.getInstance();
        final DeploymentManager deployment = Servlets.defaultContainer()
            .addDeployment(
                Servlets.deployment()
                    .setClassLoader(UndertowTest.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName(UndertowTest.class.getName())
                    .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                            .addEndpoint(serverEndpointConfig())
                            .setWorker(xnio.createWorker(OptionMap.builder().getMap()))
                            .setBuffers(new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)))
                    )
            );
        deployment.deploy();
        final Undertow server = Undertow.builder()
            .addHttpListener(PORT, "localhost")
            .setHandler(deployment.start())
            .build();
        server.start();
        connect(
            new ServerWebSocketContainer(
                DefaultClassIntrospector.INSTANCE,
                xnio.createWorker(OptionMap.create(Options.THREAD_DAEMON, true)),
                new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)),
                Collections.singletonList(new ContextClassLoaderSetupAction(ClassLoader.getSystemClassLoader())),
                true,
                true
            ),
            latch
        );
        server.stop();
    }

}
