package ch.softappeal.yass.transport;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.Dispatcher;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;

public class TransportSetup extends SessionSetup {

    /**
     * Uses {@link PacketSerializer} and {@link MessageSerializer}.
     */
    public static Serializer packetSerializer(final Serializer contractSerializer) {
        return new PacketSerializer(new MessageSerializer(contractSerializer));
    }

    /**
     * A {@link Serializer} for {@link Packet}.
     */
    public final Serializer packetSerializer;

    public TransportSetup(final Server server, final Dispatcher dispatcher, final Serializer packetSerializer, final SessionFactory sessionFactory) {
        super(server, dispatcher, sessionFactory);
        this.packetSerializer = Check.notNull(packetSerializer);
    }

    /**
     * Uses {@link #packetSerializer(Serializer)}.
     */
    public static TransportSetup create(final Server server, final Dispatcher dispatcher, final Serializer contractSerializer, final SessionFactory sessionFactory) {
        return new TransportSetup(server, dispatcher, packetSerializer(contractSerializer), sessionFactory);
    }

    public static Dispatcher dispatcher(final Executor executor) {
        Check.notNull(executor);
        return new Dispatcher() {
            @Override public void opened(final Session session, final Runnable runnable) {
                executor.execute(runnable);
            }
            @Override public void invoke(final Session session, final Server.Invocation invocation, final Runnable runnable) {
                executor.execute(runnable);
            }
        };
    }

    public TransportSetup(final Server server, final Executor dispatcherExecutor, final Serializer packetSerializer, final SessionFactory sessionFactory) {
        this(server, dispatcher(dispatcherExecutor), packetSerializer, sessionFactory);
    }

    /**
     * Uses {@link #packetSerializer(Serializer)}.
     */
    public static TransportSetup create(final Server server, final Executor dispatcherExecutor, final Serializer contractSerializer, final SessionFactory sessionFactory) {
        return new TransportSetup(server, dispatcherExecutor, packetSerializer(contractSerializer), sessionFactory);
    }

}
