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
 * Adapts a contract {@link Serializer} to one for {@link Message}.
 * Note: {@link Request#arguments} is serialized as a {@link List}.
 */
public final class MessageSerializer implements Serializer {

    public final Serializer contractSerializer;

    public MessageSerializer(final Serializer contractSerializer) {
        this.contractSerializer = Check.notNull(contractSerializer);
    }

    private static final byte REQUEST = (byte)0;
    private static final byte VALUE_REPLY = (byte)1;
    private static final byte EXCEPTION_REPLY = (byte)2;

    private static Object[] toArray(final List<Object> list) {
        return list.toArray(new Object[list.size()]);
    }

    @SuppressWarnings("unchecked")
    @Override public Message read(final Reader reader) throws Exception {
        final byte type = reader.readByte();
        if (type == REQUEST) {
            return new Request(reader.readZigZagInt(), reader.readZigZagInt(), toArray((List<Object>)contractSerializer.read(reader)));
        }
        if (type == VALUE_REPLY) {
            return new ValueReply(
                contractSerializer.read(reader)
            );
        }
        return new ExceptionReply(
            (Throwable)contractSerializer.read(reader)
        );
    }

    private static final List<?> NO_ARGUMENTS = Collections.emptyList();

    @Override public void write(final Object message, final Writer writer) throws Exception {
        if (message instanceof Request) {
            writer.writeByte(REQUEST);
            final Request request = (Request)message;
            writer.writeZigZagInt(request.serviceId);
            writer.writeZigZagInt(request.methodId);
            contractSerializer.write((request.arguments == null) ? NO_ARGUMENTS : Arrays.asList(request.arguments), writer);
        } else if (message instanceof ValueReply) {
            writer.writeByte(VALUE_REPLY);
            final ValueReply reply = (ValueReply)message;
            contractSerializer.write(reply.value, writer);
        } else {
            writer.writeByte(EXCEPTION_REPLY);
            final ExceptionReply reply = (ExceptionReply)message;
            contractSerializer.write(reply.throwable, writer);
        }
    }

}
