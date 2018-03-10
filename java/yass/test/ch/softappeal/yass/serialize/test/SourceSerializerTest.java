package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.transport.SourceSerializer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class SourceSerializerTest {

    private static void check(final boolean initiator, final byte[] wire) throws Exception {
        final Serializer contractSerializer = new SimpleFastSerializer(
            List.of(BaseTypeHandlers.BYTE_ARRAY),
            List.of(),
            List.of()
        );
        final Serializer serializer = new PrinterSerializer(
            initiator ? SourceSerializer.initiator(contractSerializer) : SourceSerializer.acceptor(contractSerializer)
        );
        final byte[] raw = new byte[] {0};
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.write(raw, Writer.create(out));
        final byte[] bytes = out.toByteArray();
        Assert.assertArrayEquals(bytes, wire);
        Assert.assertArrayEquals((byte[])serializer.read(Reader.create(new ByteArrayInputStream(bytes))), raw);
    }

    @Test public void testInitiator() throws Exception {
        check(true, new byte[] {0, 3, 1, 0});
    }

    @Test public void testAcceptor() throws Exception {
        check(false, new byte[] {1, 3, 1, 0});
    }

}
