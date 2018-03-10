package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.transport.FrameSerializer;
import ch.softappeal.yass.util.Instantiators;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class FrameSerializerTest {

    private static final Serializer SERIALIZER = new PrinterSerializer(new FrameSerializer(new SimpleFastSerializer(
        Instantiators.NOARG,
        List.of(BaseTypeHandlers.BYTE_ARRAY),
        List.of(),
        List.of()
    )));

    private static void check(final byte[] raw, final byte[] wire) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        SERIALIZER.write(raw, Writer.create(out));
        final byte[] bytes = out.toByteArray();
        Assert.assertArrayEquals(bytes, wire);
        Assert.assertArrayEquals((byte[])SERIALIZER.read(Reader.create(new ByteArrayInputStream(bytes))), raw);
    }

    @Test public void test() throws Exception {
        check(new byte[] {0}, new byte[] {-1, 3, 1, 0});
        check(new byte[] {-1}, new byte[] {-1, 3, 1, -2, 1});
        check(new byte[] {-2}, new byte[] {-1, 3, 1, -2, 0});
    }

}
