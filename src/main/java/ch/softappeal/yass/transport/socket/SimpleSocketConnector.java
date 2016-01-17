package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;

import javax.net.SocketFactory;
import java.net.Socket;
import java.net.SocketAddress;

public final class SimpleSocketConnector implements SocketConnector {

    private final SocketFactory socketFactory;
    private final int connectTimeoutMilliSeconds;
    private final int readTimeoutMilliSeconds;
    private final SocketAddress socketAddress;

    /**
     * @param connectTimeoutMilliSeconds see {@link Socket#connect(SocketAddress, int)}
     * @param readTimeoutMilliSeconds see {@link Socket#setSoTimeout(int)}
     */
    public SimpleSocketConnector(final SocketFactory socketFactory, final int connectTimeoutMilliSeconds, final int readTimeoutMilliSeconds, final SocketAddress socketAddress) {
        this.socketFactory = Check.notNull(socketFactory);
        if (connectTimeoutMilliSeconds < 0) {
            throw new IllegalArgumentException("connectTimeoutMilliSeconds < 0");
        }
        this.connectTimeoutMilliSeconds = connectTimeoutMilliSeconds;
        if (readTimeoutMilliSeconds < 0) {
            throw new IllegalArgumentException("readTimeoutMilliSeconds < 0");
        }
        this.readTimeoutMilliSeconds = readTimeoutMilliSeconds;
        this.socketAddress = Check.notNull(socketAddress);
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

    public SimpleSocketConnector(final SocketFactory socketFactory, final int connectTimeoutMilliSeconds, final SocketAddress socketAddress) {
        this(socketFactory, connectTimeoutMilliSeconds, 0, socketAddress);
    }

    public SimpleSocketConnector(final SocketFactory socketFactory, final SocketAddress socketAddress) {
        this(socketFactory, 0, socketAddress);
    }

    public SimpleSocketConnector(final int connectTimeoutMilliSeconds, final int readTimeoutMilliSeconds, final SocketAddress socketAddress) {
        this(SocketFactory.getDefault(), connectTimeoutMilliSeconds, readTimeoutMilliSeconds, socketAddress);
    }

    public SimpleSocketConnector(final int connectTimeoutMilliSeconds, final SocketAddress socketAddress) {
        this(connectTimeoutMilliSeconds, 0, socketAddress);
    }

    public SimpleSocketConnector(final SocketAddress socketAddress) {
        this(0, socketAddress);
    }

}
