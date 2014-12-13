package ch.softappeal.yass.transport;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;

public class TransportSetup extends SessionSetup {

    public final Serializer packetSerializer;

    public TransportSetup(final Server server, final Executor requestExecutor, final Serializer packetSerializer, final SessionFactory sessionFactory) {
        super(server, requestExecutor, sessionFactory);
        this.packetSerializer = Check.notNull(packetSerializer);
    }

    public TransportSetup(final SessionSetup setup, final Serializer packetSerializer) {
        this(setup.server, setup.requestExecutor, packetSerializer, setup.sessionFactory);
    }

}
