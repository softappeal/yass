package test;

import org.eclipse.jetty.websocket.jsr356.ClientContainer;

public final class JettyInitiator {

    public static void main(final String... args) throws Exception {
        final ClientContainer container = new ClientContainer();
        container.start();
        Initiator.run(container);
    }

}
