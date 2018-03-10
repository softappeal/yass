package test;

import org.eclipse.jetty.websocket.jsr356.ClientContainer;

public final class JettyClient {

    public static void main(final String... args) throws Exception {
        final var container = new ClientContainer();
        container.start();
        Client.run(container);
    }

}
