package ch.softappeal.yass.transport;

import ch.softappeal.yass.serialize.CompositeSerializer;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Nullable;

/**
 * Adds a source byte for making the originator visible on the wire.
 */
public final class SourceSerializer extends CompositeSerializer {

    private final byte source;

    private SourceSerializer(final Serializer serializer, final byte source) {
        super(serializer);
        this.source = source;
    }

    @Override public void write(final @Nullable Object value, final Writer writer) throws Exception {
        writer.writeByte(source);
        serializer.write(value, writer);
    }

    @Override public @Nullable Object read(final Reader reader) throws Exception {
        reader.readByte(); // skip
        return serializer.read(reader);
    }

    public static Serializer initiator(final Serializer serializer) {
        return new SourceSerializer(serializer, (byte)0);
    }

    public static Serializer acceptor(final Serializer serializer) {
        return new SourceSerializer(serializer, (byte)1);
    }

}
