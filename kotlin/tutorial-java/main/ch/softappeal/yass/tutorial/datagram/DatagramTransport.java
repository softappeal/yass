package ch.softappeal.yass.tutorial.datagram;

import ch.softappeal.yass.remote.Client;
import ch.softappeal.yass.remote.ClientInvocation;
import ch.softappeal.yass.remote.MethodMapping;
import ch.softappeal.yass.remote.Request;
import ch.softappeal.yass.remote.ServerInvocation;
import ch.softappeal.yass.serialize.ByteBufferOutputStream;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.ServerTransport;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Objects;

import static ch.softappeal.yass.serialize.ReaderKt.reader;
import static ch.softappeal.yass.serialize.WriterKt.writer;

public final class DatagramTransport {

    private DatagramTransport() {
        // disable
    }

    private static void checkOneWay(final MethodMapping methodMapping, final Request request) {
        if (!methodMapping.getOneWay()) {
            throw new IllegalArgumentException(
                "transport not allowed for rpc method (serviceId " + request.getServiceId() + ", methodId " + request.getMethodId() + ')'
            );
        }
    }

    public static Client client(final Serializer messageSerializer, final DatagramChannel channel, final SocketAddress target) {
        Objects.requireNonNull(messageSerializer);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(target);
        return new Client() {
            @Override public void invoke(final ClientInvocation invocation) {
                invocation.invoke(false, request -> {
                    checkOneWay(invocation.getMethodMapping(), request);
                    final ByteBufferOutputStream out = new ByteBufferOutputStream(128);
                    messageSerializer.write(writer(out), request);
                    try {
                        channel.send(out.toByteBuffer(), target);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                });
            }
        };
    }

    public static void invoke(final ServerTransport setup, final DatagramChannel channel, final int maxRequestBytes) throws Exception {
        final ByteBuffer in = ByteBuffer.allocate(maxRequestBytes);
        channel.receive(in);
        in.flip();
        final Request request = setup.read(reader(in));
        if (in.hasRemaining()) {
            throw new RuntimeException("input buffer is not empty");
        }
        final ServerInvocation invocation = setup.invocation(false, request);
        checkOneWay(invocation.getMethodMapping(), request);
        invocation.invoke(reply -> null);
    }

}
