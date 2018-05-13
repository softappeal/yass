package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.tutorial.shared.SslConfig;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.XnioByteBufferPool;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Xnio;

import java.io.File;

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
                            .setBuffers(new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)))
                    )
            );
        deployment.deploy();
        final HttpHandler servletHandler = deployment.start();
        final ResourceHandler fileHandler = Handlers.resource(new FileResourceManager(new File(WEB_PATH), 100));
        Undertow.builder()
            .addHttpListener(PORT, HOST)
            .addHttpsListener(PORT + 1, HOST, SslConfig.SERVER.getContext()) // note: we don't know how to force client authentication
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
