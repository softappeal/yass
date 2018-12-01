package ch.softappeal.yass.transport.ws.container;

import org.eclipse.jetty.websocket.jsr356.*;

public final class JettyClient {

    public static void main(final String... args) throws Exception {
        final ClientContainer container = new ClientContainer();
        container.start();
        Client.run(container);
    }

}
