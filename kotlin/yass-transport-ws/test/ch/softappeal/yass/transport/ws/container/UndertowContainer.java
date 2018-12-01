package ch.softappeal.yass.transport.ws.container;

import io.undertow.*;
import io.undertow.server.*;
import io.undertow.servlet.*;
import io.undertow.servlet.api.*;
import io.undertow.websockets.jsr.*;
import org.xnio.*;

public final class UndertowContainer {

    public static void main(final String... args) throws Exception {
        final DeploymentManager deployment = Servlets.defaultContainer()
            .addDeployment(
                Servlets.deployment()
                    .setClassLoader(UndertowContainer.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName(UndertowContainer.class.getName())
                    .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                            .addEndpoint(ApplicationConfig.ENDPOINT_CONFIG)
                            .setWorker(Xnio.getInstance().createWorker(OptionMap.builder().getMap()))
                            .setBuffers(
                                new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240))
                            )
                    )
            );
        deployment.deploy();
        final HttpHandler servletHandler = deployment.start();
        Undertow.builder()
            .addHttpListener(Client.PORT, Client.HOST)
            .setHandler(servletHandler)
            .build()
            .start();
    }

}
