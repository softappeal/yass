package ch.softappeal.yass.transport;

import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.serialize.Serializer;

import java.util.Objects;

public final class SimpleTransportSetup {

    /**
     * A {@link Serializer} for {@link Message}.
     */
    public final Serializer messageSerializer;

    public final Server server;

    public SimpleTransportSetup(final Serializer messageSerializer, final Server server) {
        this.messageSerializer = Objects.requireNonNull(messageSerializer);
        this.server = Objects.requireNonNull(server);
    }

}
