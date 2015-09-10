package ch.softappeal.yass.transport;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;

public final class StringPathSerializer implements Serializer {

    public static final String DEFAULT = "<DEFAULT>";

    private StringPathSerializer() {
        // disable
    }

    @Override public Object read(final Reader reader) throws Exception {
        return BaseTypeHandlers.STRING.read(reader);
    }

    @Override public void write(final Object value, final Writer writer) throws Exception {
        BaseTypeHandlers.STRING.write((String)value, writer);

    }

    public static final Serializer INSTANCE = new StringPathSerializer();

}
