package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Exceptions;

import javax.net.ServerSocketFactory;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Supplier;

@FunctionalInterface public interface SocketBinder extends Supplier<ServerSocket> {

    static SocketBinder create(final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        Objects.requireNonNull(socketFactory);
        Objects.requireNonNull(socketAddress);
        return () -> {
            try {
                final var serverSocket = socketFactory.createServerSocket();
                try {
                    serverSocket.bind(socketAddress);
                    return serverSocket;
                } catch (final Exception e) {
                    SocketUtils.close(serverSocket, e);
                    throw e;
                }
            } catch (final Exception e) {
                throw Exceptions.wrap(e);
            }
        };
    }

    static SocketBinder create(final SocketAddress socketAddress) {
        return create(ServerSocketFactory.getDefault(), socketAddress);
    }

}
