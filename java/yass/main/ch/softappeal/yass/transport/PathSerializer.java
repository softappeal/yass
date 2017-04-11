package ch.softappeal.yass.transport;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;

/**
 * Writes path as an {@link Integer}.
 */
public final class PathSerializer implements Serializer {

    public static final Integer DEFAULT = 0;

    private PathSerializer() {
        // disable
    }

    @Override public Object read(final Reader reader) throws Exception {
        return reader.readInt();
    }

    @Override public void write(final Object value, final Writer writer) throws Exception {
        writer.writeInt((Integer)value);
    }

    public static final Serializer INSTANCE = new PathSerializer();

}
