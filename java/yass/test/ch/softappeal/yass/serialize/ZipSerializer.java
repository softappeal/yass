package ch.softappeal.yass.serialize;

import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.util.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * A serializer using {@link InflaterInputStream} and {@link DeflaterOutputStream}.
 */
public final class ZipSerializer extends CompositeSerializer {

    public ZipSerializer(final Serializer serializer) {
        super(serializer);
    }

    @Override public @Nullable Object read(final Reader reader) throws Exception {
        try (InputStream in = new InflaterInputStream(new ByteArrayInputStream(BaseTypeHandlers.BYTE_ARRAY.read(reader)))) {
            return serializer.read(Reader.create(in));
        }
    }

    @Override public void write(final @Nullable Object value, final Writer writer) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream(128);
        try (OutputStream out = new DeflaterOutputStream(bytes)) {
            serializer.write(value, Writer.create(out));
        }
        BaseTypeHandlers.BYTE_ARRAY.write(bytes.toByteArray(), writer);
    }

}
