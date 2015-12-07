package ch.softappeal.yass.transport.test;

import ch.softappeal.yass.core.remote.ExceptionReply;
import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.ValueReply;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.test.JavaSerializerTest;
import ch.softappeal.yass.transport.MessageSerializer;
import org.junit.Assert;
import org.junit.Test;

import java.io.EOFException;

public class MessageSerializerTest {

    private static final Serializer SERIALIZER = new MessageSerializer(JavaSerializer.INSTANCE);

    private static <T extends Message> T copy(final T value) throws Exception {
        return JavaSerializerTest.copy(SERIALIZER, value);
    }

    @Test public void request() throws Exception {
        final int serviceId = 123456;
        final int methodId = 4711;
        final Request request = copy(new Request(serviceId, methodId, new Object[0]));
        Assert.assertEquals(serviceId, request.serviceId);
        Assert.assertEquals(methodId, request.methodId);
        Assert.assertTrue(request.arguments.length == 0);
    }

    @Test public void value() throws Exception {
        final String value = "xyz";
        final ValueReply reply = copy(new ValueReply(value));
        Assert.assertEquals(value, reply.value);
    }

    @Test public void exception() throws Exception {
        final ExceptionReply reply = copy(new ExceptionReply(new EOFException()));
        Assert.assertTrue(reply.throwable instanceof EOFException);
    }

}
