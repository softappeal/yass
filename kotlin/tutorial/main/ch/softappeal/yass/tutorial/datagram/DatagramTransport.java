package ch.softappeal.yass.tutorial.datagram;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.serialize.*;
import ch.softappeal.yass.transport.*;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import static ch.softappeal.yass.serialize.ReaderKt.*;
import static ch.softappeal.yass.serialize.WriterKt.*;

public final class DatagramTransport {

    private DatagramTransport() {
        // disable
    }

    private static void checkOneWay(final MethodMapping methodMapping, final Request request) {
        if (!methodMapping.getOneWay()) {
            throw new IllegalArgumentException(
                "transport not allowed for rpc method (serviceId " + request.getServiceId() +
                    ", methodId " + request.getMethodId() + ')'
            );
        }
    }

    public static Client client(
        final Serializer messageSerializer, final DatagramChannel channel, final SocketAddress target
    ) {
        Objects.requireNonNull(messageSerializer);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(target);
        return new Client() {
            @Override
            public void invoke(final ClientInvocation invocation) {
                invocation.invoke(false, request -> {
                    checkOneWay(invocation.getMethodMapping(), request);
                    final ByteBufferOutputStream out = new ByteBufferOutputStream(128);
                    try {
                        messageSerializer.write(writer(out), request);
                        channel.send(out.toByteBuffer(), target);
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                });
            }
        };
    }

    public static void invoke(
        final ServerTransport setup, final DatagramChannel channel, final int maxRequestBytes
    ) throws Exception {
        final ByteBuffer in = ByteBuffer.allocate(maxRequestBytes);
        channel.receive(in);
        in.flip();
        final Request request = setup.read(reader(in));
        if (in.hasRemaining()) {
            throw new RuntimeException("input buffer is not empty");
        }
        final ServerInvocation invocation = setup.invocation(true, request);
        checkOneWay(invocation.getMethodMapping(), request);
        invocation.invoke(reply -> null);
    }

}
