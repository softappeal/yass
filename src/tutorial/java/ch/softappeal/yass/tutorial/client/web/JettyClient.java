package ch.softappeal.yass.tutorial.client.web;

import org.eclipse.jetty.websocket.jsr356.ClientContainer;

public final class JettyClient extends WsClientSetup {

    public static void main(final String... args) throws Exception {
        final ClientContainer container = new ClientContainer();
        container.start();
        run(container);
    }

}
