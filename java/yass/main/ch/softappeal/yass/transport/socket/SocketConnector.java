package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Exceptions;

import javax.net.SocketFactory;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Supplier;

@FunctionalInterface public interface SocketConnector extends Supplier<Socket> {

    /**
     * @param connectTimeoutMilliSeconds see {@link Socket#connect(SocketAddress, int)}
     * @param readTimeoutMilliSeconds see {@link Socket#setSoTimeout(int)}
     */
    static SocketConnector create(final SocketFactory socketFactory, final SocketAddress socketAddress, final int connectTimeoutMilliSeconds, final int readTimeoutMilliSeconds) {
        Objects.requireNonNull(socketFactory);
        Objects.requireNonNull(socketAddress);
        if (connectTimeoutMilliSeconds < 0) {
            throw new IllegalArgumentException("connectTimeoutMilliSeconds < 0");
        }
        if (readTimeoutMilliSeconds < 0) {
            throw new IllegalArgumentException("readTimeoutMilliSeconds < 0");
        }
        return () -> {
            try {
                final var socket = socketFactory.createSocket();
                try {
                    socket.connect(socketAddress, connectTimeoutMilliSeconds);
                    socket.setSoTimeout(readTimeoutMilliSeconds);
                    return socket;
                } catch (final Exception e) {
                    SocketUtils.close(socket, e);
                    throw e;
                }
            } catch (final Exception e) {
                throw Exceptions.wrap(e);
            }
        };
    }

    static SocketConnector create(final SocketFactory socketFactory, final SocketAddress socketAddress, final int connectTimeoutMilliSeconds) {
        return create(socketFactory, socketAddress, connectTimeoutMilliSeconds, 0);
    }

    static SocketConnector create(final SocketFactory socketFactory, final SocketAddress socketAddress) {
        return create(socketFactory, socketAddress, 0);
    }

    static SocketConnector create(final SocketAddress socketAddress, final int connectTimeoutMilliSeconds, final int readTimeoutMilliSeconds) {
        return create(SocketFactory.getDefault(), socketAddress, connectTimeoutMilliSeconds, readTimeoutMilliSeconds);
    }

    static SocketConnector create(final SocketAddress socketAddress, final int connectTimeoutMilliSeconds) {
        return create(socketAddress, connectTimeoutMilliSeconds, 0);
    }

    static SocketConnector create(final SocketAddress socketAddress) {
        return create(socketAddress, 0);
    }

}
