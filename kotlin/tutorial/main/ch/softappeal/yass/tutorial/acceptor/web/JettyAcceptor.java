package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.tutorial.shared.*;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.util.ssl.*;
import org.eclipse.jetty.websocket.jsr356.server.deploy.*;

public final class JettyAcceptor extends WebAcceptorSetup {

    public static void main(final String... args) throws Exception {
        final Server server = new Server();
        final ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setHost(HOST);
        serverConnector.setPort(PORT);
        server.addConnector(serverConnector);
        final HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
        final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setSslContext(SslConfig.SERVER.getContext());
        sslContextFactory.setNeedClientAuth(true);
        final ServerConnector sslServerConnector = new ServerConnector(
            server,
            new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()),
            new HttpConnectionFactory(https)
        );
        sslServerConnector.setHost(HOST);
        sslServerConnector.setPort(PORT + 1);
        server.addConnector(sslServerConnector);
        final ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(new XhrServlet()), XHR_PATH);
        final ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setResourceBase(WEB_PATH);
        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resourceHandler, contextHandler});
        server.setHandler(handlers);
        WebSocketServerContainerInitializer.configureContext(contextHandler).addEndpoint(ENDPOINT_CONFIG);
        server.start();
        System.out.println("started");
    }

}
