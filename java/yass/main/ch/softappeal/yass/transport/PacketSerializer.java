package ch.softappeal.yass.transport;

import ch.softappeal.yass.remote.Message;
import ch.softappeal.yass.remote.session.Packet;
import ch.softappeal.yass.serialize.CompositeSerializer;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;

/**
 * Default {@link Serializer} for {@link Packet}.
 */
public final class PacketSerializer extends CompositeSerializer {

    /**
     * @param messageSerializer A {@link Serializer} for {@link Message}.
     */
    public PacketSerializer(final Serializer messageSerializer) {
        super(messageSerializer);
    }

    @Override public Packet read(final Reader reader) throws Exception {
        final var requestNumber = reader.readInt();
        return Packet.isEnd(requestNumber) ? Packet.END : new Packet(requestNumber, (Message)serializer.read(reader));
    }

    @Override public void write(final Object value, final Writer writer) throws Exception {
        final var packet = (Packet)value;
        if (packet.isEnd()) {
            writer.writeInt(Packet.END_REQUEST_NUMBER);
        } else {
            writer.writeInt(packet.requestNumber());
            serializer.write(packet.message(), writer);
        }
    }

}
