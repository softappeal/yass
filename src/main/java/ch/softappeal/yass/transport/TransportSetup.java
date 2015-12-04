package ch.softappeal.yass.transport;

import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.util.Check;

public final class TransportSetup {

    /**
     * A {@link Serializer} for {@link Packet}.
     */
    public final Serializer packetSerializer;

    public final SessionFactory sessionFactory;

    private TransportSetup(final Serializer packetSerializer, final SessionFactory sessionFactory) {
        this.packetSerializer = Check.notNull(packetSerializer);
        this.sessionFactory = Check.notNull(sessionFactory);
    }

    public static TransportSetup ofPacketSerializer(final Serializer packetSerializer, final SessionFactory sessionFactory) {
        return new TransportSetup(packetSerializer, sessionFactory);
    }

    /**
     * Uses {@link PacketSerializer} and {@link MessageSerializer}.
     */
    public static TransportSetup ofContractSerializer(final Serializer contractSerializer, final SessionFactory sessionFactory) {
        return new TransportSetup(new PacketSerializer(new MessageSerializer(contractSerializer)), sessionFactory);
    }

}
