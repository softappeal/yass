package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.ZipSerializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ZipSerializerTest {

    private static final Serializer BYTES_SERIALIZER = new Serializer() {
        @Override public @Nullable Object read(final Reader reader) throws Exception {
            return BaseTypeHandlers.BYTE_ARRAY.read(reader);
        }
        @Override public void write(final @Nullable Object value, final Writer writer) throws Exception {
            BaseTypeHandlers.BYTE_ARRAY.write((byte[])value, writer);
        }
    };

    private static final Serializer SERIALIZER = new PrinterSerializer(new ZipSerializer(new PrinterSerializer(BYTES_SERIALIZER)));

    private static void check(final byte[] value) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        SERIALIZER.write(value, Writer.create(out));
        final byte[] bytes = out.toByteArray();
        Assert.assertArrayEquals((byte[])SERIALIZER.read(Reader.create(new ByteArrayInputStream(bytes))), value);
    }

    @Test public void test() throws Exception {
        check(new byte[0]);
        check(new byte[] {1, 2, 3, 4, 5});
        check(new byte[100]);
    }

}
