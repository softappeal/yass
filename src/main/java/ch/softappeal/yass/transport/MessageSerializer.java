package ch.softappeal.yass.transport;

import ch.softappeal.yass.core.remote.ExceptionReply;
import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.ValueReply;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Check;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Adapts a {@link Serializer} for {@link Object} to one for {@link Message}.
 * Note: {@link Request#arguments} is serialized as a {@link List}.
 */
public final class MessageSerializer implements Serializer {

  private final Serializer serializer;

  public MessageSerializer(final Serializer serializer) {
    this.serializer = Check.notNull(serializer);
  }

  private static final byte REQUEST = 0;
  private static final byte VALUE_REPLY = 1;
  private static final byte EXCEPTION_REPLY = 2;

  private static Object[] toArray(final List<Object> list) {
    return list.toArray(new Object[list.size()]);
  }

  @SuppressWarnings({"unchecked", "IfMayBeConditional"})
  @Override public Message read(final Reader reader) throws Exception {
    final byte type = reader.readByte();
    if (type == REQUEST) {
      //noinspection rawtypes
      return new Request(
        serializer.read(reader),
        serializer.read(reader),
        toArray((List)serializer.read(reader))
      );
    } else if (type == VALUE_REPLY) {
      return new ValueReply(
        serializer.read(reader)
      );
    } else {
      return new ExceptionReply(
        (Throwable)serializer.read(reader)
      );
    }
  }

  private static final List<?> NO_ARGUMENTS = Collections.emptyList();

  @Override public void write(final Object message, final Writer writer) throws Exception {
    //noinspection ChainOfInstanceofChecks
    if (message instanceof Request) {
      writer.writeByte(REQUEST);
      final Request request = (Request)message;
      serializer.write(request.serviceId, writer);
      serializer.write(request.methodId, writer);
      serializer.write((request.arguments == null) ? NO_ARGUMENTS : Arrays.asList(request.arguments), writer);
    } else if (message instanceof ValueReply) {
      writer.writeByte(VALUE_REPLY);
      final ValueReply reply = (ValueReply)message;
      serializer.write(reply.value, writer);
    } else {
      writer.writeByte(EXCEPTION_REPLY);
      final ExceptionReply reply = (ExceptionReply)message;
      serializer.write(reply.throwable, writer);
    }
  }

}
