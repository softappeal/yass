package ch.softappeal.yass.transport;

import ch.softappeal.yass.remote.session.Packet;
import ch.softappeal.yass.remote.session.SessionFactory;
import ch.softappeal.yass.serialize.Serializer;

import java.util.Objects;

public final class TransportSetup {

    /**
     * A {@link Serializer} for {@link Packet}.
     */
    public final Serializer packetSerializer;

    public final SessionFactory sessionFactory;

    private TransportSetup(final Serializer packetSerializer, final SessionFactory sessionFactory) {
        this.packetSerializer = Objects.requireNonNull(packetSerializer);
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
    }

    public static TransportSetup ofPacketSerializer(final Serializer packetSerializer, final SessionFactory sessionFactory) {
        return new TransportSetup(packetSerializer, sessionFactory);
    }

    public static TransportSetup ofContractSerializer(final Serializer contractSerializer, final SessionFactory sessionFactory) {
        return new TransportSetup(new PacketSerializer(new MessageSerializer(contractSerializer)), sessionFactory);
    }

}
