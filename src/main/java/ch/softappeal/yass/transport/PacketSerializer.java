package ch.softappeal.yass.transport;

import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Check;

/**
 * Adapts a {@link Serializer} for {@link Message} to one for {@link Packet}.
 */
public final class PacketSerializer implements Serializer {

    public final Serializer messageSerializer;

    public PacketSerializer(final Serializer messageSerializer) {
        this.messageSerializer = Check.notNull(messageSerializer);
    }

    @Override public Packet read(final Reader reader) throws Exception {
        final int requestNumber = reader.readInt();
        return Packet.isEnd(requestNumber) ? Packet.END : new Packet(requestNumber, (Message)messageSerializer.read(reader));
    }

    @Override public void write(final Object value, final Writer writer) throws Exception {
        final Packet packet = (Packet)value;
        if (packet.isEnd()) {
            writer.writeInt(Packet.END_REQUEST_NUMBER);
        } else {
            writer.writeInt(packet.requestNumber());
            messageSerializer.write(packet.message(), writer);
        }
    }

}
