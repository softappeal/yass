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

  public static final Serializer SERIALIZER = new MessageSerializer(JavaSerializer.INSTANCE);

  private static <T extends Message> T copy(final T value) throws Exception {
    return JavaSerializerTest.copy(SERIALIZER, value);
  }

  @Test public void request() throws Exception {
    final Object serviceId = "abc";
    final String methodId = "xyz";
    final Request request = copy(new Request(null, serviceId, methodId, new Object[0]));
    Assert.assertNull(request.context);
    Assert.assertEquals(serviceId, request.serviceId);
    Assert.assertEquals(methodId, request.methodId);
    Assert.assertTrue(request.arguments.length == 0);
  }

  @Test public void value() throws Exception {
    final String value = "xyz";
    final ValueReply reply = copy(new ValueReply(null, value));
    Assert.assertNull(reply.context);
    Assert.assertEquals(value, reply.value);
  }

  @Test public void exception() throws Exception {
    final ExceptionReply reply = copy(new ExceptionReply(null, new EOFException()));
    Assert.assertNull(reply.context);
    Assert.assertTrue(reply.throwable instanceof EOFException);
  }

}
