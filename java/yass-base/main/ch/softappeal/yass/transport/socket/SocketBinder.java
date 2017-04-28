package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import javax.net.ServerSocketFactory;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Supplier;

@FunctionalInterface public interface SocketBinder extends Supplier<ServerSocket> {

    static SocketBinder create(final ServerSocketFactory socketFactory, final SocketAddress socketAddress, final @Nullable Boolean reuseAddress) {
        Objects.requireNonNull(socketFactory);
        Objects.requireNonNull(socketAddress);
        return () -> {
            try {
                final ServerSocket serverSocket = socketFactory.createServerSocket();
                try {
                    if (reuseAddress != null) {
                        serverSocket.setReuseAddress(reuseAddress);
                    }
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

    static SocketBinder create(final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        return create(socketFactory, socketAddress, null);
    }

    static SocketBinder create(final SocketAddress socketAddress) {
        return create(ServerSocketFactory.getDefault(), socketAddress);
    }

}
