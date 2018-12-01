package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.tutorial.shared.*;
import io.undertow.*;
import io.undertow.server.*;
import io.undertow.server.handlers.resource.*;
import io.undertow.servlet.*;
import io.undertow.servlet.api.*;
import io.undertow.websockets.jsr.*;
import org.xnio.*;

import java.io.*;

public final class UndertowAcceptor extends WebAcceptorSetup {

    public static void main(final String... args) throws Exception {
        final DeploymentManager deployment = Servlets.defaultContainer()
            .addDeployment(
                Servlets.deployment()
                    .setClassLoader(UndertowAcceptor.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName(UndertowAcceptor.class.getName())
                    .addServlet(
                        Servlets.servlet("Xhr", XhrServlet.class)
                            .addMapping(XHR_PATH)
                    )
                    .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                            .addEndpoint(ENDPOINT_CONFIG)
                            .setWorker(Xnio.getInstance().createWorker(OptionMap.builder().getMap()))
                            .setBuffers(
                                new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240))
                            )
                    )
            );
        deployment.deploy();
        final HttpHandler servletHandler = deployment.start();
        final ResourceHandler fileHandler =
            Handlers.resource(new FileResourceManager(new File(WEB_PATH), 100));
        Undertow.builder()
            .addHttpListener(PORT, HOST)
            .addHttpsListener( // note: we don't know how to force client authentication
                PORT + 1, HOST, SslConfig.SERVER.getContext()
            )
            .setHandler(exchange -> {
                final String path = exchange.getRequestPath();
                if (WS_PATH.equals(path) || XHR_PATH.equals(path)) {
                    servletHandler.handleRequest(exchange);
                } else {
                    fileHandler.handleRequest(exchange);
                }
            })
            .build()
            .start();
        System.out.println("started");
    }

}
