package ch.softappeal.yass.transport.test;

import ch.softappeal.yass.remote.ExceptionReply;
import ch.softappeal.yass.remote.Message;
import ch.softappeal.yass.remote.Request;
import ch.softappeal.yass.remote.ValueReply;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.test.JavaSerializerTest;
import ch.softappeal.yass.transport.MessageSerializer;
import org.junit.Assert;
import org.junit.Test;

import java.io.EOFException;
import java.util.List;

public class MessageSerializerTest {

    private static final Serializer SERIALIZER = new MessageSerializer(JavaSerializer.INSTANCE);

    private static <T extends Message> T copy(final T value) throws Exception {
        return JavaSerializerTest.copy(SERIALIZER, value);
    }

    @Test public void request() throws Exception {
        final var serviceId = 123456;
        final var methodId = 4711;
        final var request = copy(new Request(serviceId, methodId, List.of()));
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
