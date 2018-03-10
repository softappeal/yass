package ch.softappeal.yass.tutorial.initiator.web;

import org.eclipse.jetty.websocket.jsr356.ClientContainer;

public final class JettyInitiator extends WebInitiatorSetup {

    public static void main(final String... args) throws Exception {
        final var container = new ClientContainer();
        container.start();
        run(container);
    }

}
