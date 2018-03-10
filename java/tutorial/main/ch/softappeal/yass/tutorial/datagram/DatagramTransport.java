package ch.softappeal.yass.tutorial.datagram;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.SimpleTransportSetup;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Objects;

public final class DatagramTransport {

    private DatagramTransport() {
        // disable
    }

    private static void checkOneWay(final MethodMapper.Mapping methodMapping, final Request request) {
        if (!methodMapping.oneWay) {
            throw new IllegalArgumentException(
                "transport not allowed for rpc method (serviceId " + request.serviceId + ", methodId " + request.methodId + ')'
            );
        }
    }

    public static Client client(final Serializer messageSerializer, final DatagramChannel channel, final SocketAddress target) {
        Objects.requireNonNull(messageSerializer);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(target);
        return new Client() {
            @Override public void invoke(final Invocation invocation) throws Exception {
                invocation.invoke(false, request -> {
                    checkOneWay(invocation.methodMapping, request);
                    final var out = new Writer.ByteBufferOutputStream(128);
                    messageSerializer.write(request, Writer.create(out));
                    channel.send(out.toByteBuffer(), target);
                });
            }
        };
    }

    public static void invoke(final SimpleTransportSetup setup, final DatagramChannel channel, final int maxRequestBytes) throws Exception {
        final var in = ByteBuffer.allocate(maxRequestBytes);
        channel.receive(in);
        in.flip();
        final var request = (Request)setup.messageSerializer.read(Reader.create(in));
        if (in.hasRemaining()) {
            throw new RuntimeException("input buffer is not empty");
        }
        final var invocation = setup.server.invocation(false, request);
        checkOneWay(invocation.methodMapping, request);
        invocation.invoke(reply -> {
        });
    }

}
