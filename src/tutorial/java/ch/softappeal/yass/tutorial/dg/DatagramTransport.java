package ch.softappeal.yass.tutorial.dg;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.SimpleTransportSetup;
import ch.softappeal.yass.util.Check;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public final class DatagramTransport {

    private DatagramTransport() {
        // disable
    }

    private static void checkOneWay(final MethodMapper.Mapping methodMapping, final Request request) {
        if (!methodMapping.oneWay) {
            throw new IllegalArgumentException(
                "transport not allowed for oneWay method (serviceId " + request.serviceId + ", methodId " + request.methodId + ')'
            );
        }
    }

    public static Client client(final Serializer messageSerializer, final DatagramChannel channel) {
        Check.notNull(messageSerializer);
        Check.notNull(channel);
        return new Client() {
            @Override public Object invoke(final Invocation invocation) throws Exception {
                return invocation.invoke(request -> {
                    checkOneWay(invocation.methodMapping, request);
                    final Writer.ByteBufferOutputStream out = new Writer.ByteBufferOutputStream(128);
                    messageSerializer.write(request, Writer.create(out));
                    channel.write(out.toByteBuffer());
                    return null;
                });
            }
        };
    }

    public static void invoke(final SimpleTransportSetup setup, final DatagramChannel channel, final int maxRequestBytes) throws Exception {
        final ByteBuffer in = ByteBuffer.allocate(maxRequestBytes);
        channel.receive(in);
        in.flip();
        final Request request = (Request)setup.messageSerializer.read(Reader.create(in));
        if (in.hasRemaining()) {
            throw new RuntimeException("input buffer is not empty");
        }
        final Server.Invocation invocation = setup.server.invocation(request);
        checkOneWay(invocation.methodMapping, request);
        invocation.invoke();
    }

}
