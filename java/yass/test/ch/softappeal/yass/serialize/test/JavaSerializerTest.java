package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.core.remote.ExceptionReply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.ValueReply;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.util.List;

public class JavaSerializerTest {

    @SuppressWarnings("unchecked")
    public static @Nullable <T> T copy(final Serializer serializer, final @Nullable T value) throws Exception {
        final var buffer = new ByteArrayOutputStream();
        final var writer = Writer.create(buffer);
        serializer.write(value, writer);
        writer.writeByte((byte)123); // write sentinel
        final var reader = Reader.create(new ByteArrayInputStream(buffer.toByteArray()));
        final var result = (T)serializer.read(reader);
        Assert.assertTrue(reader.readByte() == 123); // check sentinel
        return result;
    }

    private static @Nullable <T> T copy(final @Nullable T value) throws Exception {
        return copy(JavaSerializer.INSTANCE, value);
    }

    @Test public void nullValue() throws Exception {
        Assert.assertNull(copy(null));
    }

    @Test public void request() throws Exception {
        final var requestNumber = 1234567890;
        final var serviceId = 123;
        final var methodId = 1147;
        final var packet = copy(
            new Packet(
                requestNumber,
                new Request(serviceId, methodId, List.of())
            )
        );
        Assert.assertTrue(packet.requestNumber() == requestNumber);
        final var request = (Request)packet.message();
        Assert.assertEquals(serviceId, request.serviceId);
        Assert.assertEquals(methodId, request.methodId);
        Assert.assertTrue(request.arguments.isEmpty());
    }

    @Test public void value() throws Exception {
        final var value = "xyz";
        final var reply = copy(new ValueReply(value));
        Assert.assertEquals(value, reply.value);
    }

    @Test public void exception() throws Exception {
        final var reply = copy(new ExceptionReply(new EOFException()));
        Assert.assertTrue(reply.exception instanceof EOFException);
    }

}
