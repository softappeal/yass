package ch.softappeal.yass.tutorial.server.web;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Xnio;

import java.io.File;

public final class UndertowServer extends WsServerSetup {

    public static void main(final String... args) throws Exception {
        final DeploymentManager deployment = Servlets.defaultContainer()
            .addDeployment(
                Servlets.deployment()
                    .setClassLoader(UndertowServer.class.getClassLoader())
                    .setContextPath("/")
                    .setDeploymentName(UndertowServer.class.getName())
                    .addServlet(
                        Servlets.servlet("Xhr", XhrServlet.class)
                            .addMapping(XHR_PATH)
                    )
                    .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                            .addEndpoint(ENDPOINT_CONFIG)
                            .setWorker(Xnio.getInstance().createWorker(OptionMap.builder().getMap()))
                            .setBuffers(new ByteBufferSlicePool(100, 1000))
                    )
            );
        deployment.deploy();
        final HttpHandler servletHandler = deployment.start();
        final HttpHandler fileHandler = Handlers.resource(new FileResourceManager(new File(WEB_PATH), 100));
        Undertow.builder()
            .addHttpListener(PORT, HOST)
            .setHandler(exchange -> {
                final String path = exchange.getRequestPath();
                if (PATH.equals(path) || XHR_PATH.equals(path)) {
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
