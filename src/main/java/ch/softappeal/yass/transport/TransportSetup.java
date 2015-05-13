package ch.softappeal.yass.transport;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.Dispatcher;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;

public class TransportSetup extends SessionSetup {

    /**
     * A {@link Serializer} for {@link Packet}.
     */
    public final Serializer packetSerializer;

    public TransportSetup(final Server server, final Dispatcher dispatcher, final Serializer packetSerializer, final SessionFactory sessionFactory) {
        super(server, dispatcher, sessionFactory);
        this.packetSerializer = Check.notNull(packetSerializer);
    }

    public static Dispatcher dispatcher(final Executor executor) {
        Check.notNull(executor);
        return new Dispatcher() {
            @Override public void opened(final Runnable runnable) {
                executor.execute(runnable);
            }
            @Override public void invoke(final Server.Invocation invocation, final Runnable runnable) {
                executor.execute(runnable);
            }
        };
    }

    public TransportSetup(final Server server, final Executor dispatcherExecutor, final Serializer packetSerializer, final SessionFactory sessionFactory) {
        this(server, dispatcher(dispatcherExecutor), packetSerializer, sessionFactory);
    }

}
