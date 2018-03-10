package ch.softappeal.yass.transport;

import ch.softappeal.yass.core.remote.ExceptionReply;
import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.ValueReply;
import ch.softappeal.yass.serialize.CompositeSerializer;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;

import java.util.List;

/**
 * Default {@link Serializer} for {@link Message}.
 */
public final class MessageSerializer extends CompositeSerializer {

    public MessageSerializer(final Serializer contractSerializer) {
        super(contractSerializer);
    }

    private static final byte REQUEST = (byte)0;
    private static final byte VALUE_REPLY = (byte)1;
    private static final byte EXCEPTION_REPLY = (byte)2;

    @SuppressWarnings("unchecked")
    @Override public Message read(final Reader reader) throws Exception {
        final var type = reader.readByte();
        if (type == REQUEST) {
            return new Request(
                reader.readZigZagInt(),
                reader.readZigZagInt(),
                (List<Object>)serializer.read(reader)
            );
        } else if (type == VALUE_REPLY) {
            return new ValueReply(
                serializer.read(reader)
            );
        } else {
            return new ExceptionReply(
                (Exception)serializer.read(reader)
            );
        }
    }

    @Override public void write(final Object message, final Writer writer) throws Exception {
        if (message instanceof Request) {
            writer.writeByte(REQUEST);
            final var request = (Request)message;
            writer.writeZigZagInt(request.serviceId);
            writer.writeZigZagInt(request.methodId);
            serializer.write(request.arguments, writer);
        } else if (message instanceof ValueReply) {
            writer.writeByte(VALUE_REPLY);
            final var reply = (ValueReply)message;
            serializer.write(reply.value, writer);
        } else {
            writer.writeByte(EXCEPTION_REPLY);
            final var reply = (ExceptionReply)message;
            serializer.write(reply.exception, writer);
        }
    }

}
