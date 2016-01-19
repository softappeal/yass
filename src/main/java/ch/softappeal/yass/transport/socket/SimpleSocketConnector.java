package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;

import javax.net.SocketFactory;
import java.net.Socket;
import java.net.SocketAddress;

public final class SimpleSocketConnector implements SocketConnector {

    private final SocketFactory socketFactory;
    private final SocketAddress socketAddress;
    private final int connectTimeoutMilliSeconds;
    private final int readTimeoutMilliSeconds;

    /**
     * @param connectTimeoutMilliSeconds see {@link Socket#connect(SocketAddress, int)}
     * @param readTimeoutMilliSeconds see {@link Socket#setSoTimeout(int)}
     */
    public SimpleSocketConnector(final SocketFactory socketFactory, final SocketAddress socketAddress, final int connectTimeoutMilliSeconds, final int readTimeoutMilliSeconds) {
        this.socketFactory = Check.notNull(socketFactory);
        this.socketAddress = Check.notNull(socketAddress);
        if (connectTimeoutMilliSeconds < 0) {
            throw new IllegalArgumentException("connectTimeoutMilliSeconds < 0");
        }
        this.connectTimeoutMilliSeconds = connectTimeoutMilliSeconds;
        if (readTimeoutMilliSeconds < 0) {
            throw new IllegalArgumentException("readTimeoutMilliSeconds < 0");
        }
        this.readTimeoutMilliSeconds = readTimeoutMilliSeconds;
    }

    public Socket connect() throws Exception {
        final Socket socket = socketFactory.createSocket();
        try {
            socket.connect(socketAddress, connectTimeoutMilliSeconds);
            socket.setSoTimeout(readTimeoutMilliSeconds);
            return socket;
        } catch (final Exception e) {
            SocketUtils.close(socket, e);
            throw e;
        }
    }

    public SimpleSocketConnector(final SocketFactory socketFactory, final SocketAddress socketAddress, final int connectTimeoutMilliSeconds) {
        this(socketFactory, socketAddress, connectTimeoutMilliSeconds, 0);
    }

    public SimpleSocketConnector(final SocketFactory socketFactory, final SocketAddress socketAddress) {
        this(socketFactory, socketAddress, 0);
    }

    public SimpleSocketConnector(final SocketAddress socketAddress, final int connectTimeoutMilliSeconds, final int readTimeoutMilliSeconds) {
        this(SocketFactory.getDefault(), socketAddress, connectTimeoutMilliSeconds, readTimeoutMilliSeconds);
    }

    public SimpleSocketConnector(final SocketAddress socketAddress, final int connectTimeoutMilliSeconds) {
        this(socketAddress, connectTimeoutMilliSeconds, 0);
    }

    public SimpleSocketConnector(final SocketAddress socketAddress) {
        this(socketAddress, 0);
    }

}
