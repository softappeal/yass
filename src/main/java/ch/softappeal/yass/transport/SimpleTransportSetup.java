package ch.softappeal.yass.transport;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.util.Check;

public final class SimpleTransportSetup {

    public final Serializer messageSerializer;

    public final Server server;

    public SimpleTransportSetup(final Serializer messageSerializer, final Server server) {
        this.messageSerializer = Check.notNull(messageSerializer);
        this.server = Check.notNull(server);
    }

}
