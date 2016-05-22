package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;

import javax.net.ServerSocketFactory;
import java.net.ServerSocket;
import java.net.SocketAddress;

public final class SimpleSocketBinder implements SocketBinder {

    private final ServerSocketFactory socketFactory;
    private final SocketAddress socketAddress;

    @Override public ServerSocket bind() throws Exception {
        final ServerSocket serverSocket = socketFactory.createServerSocket();
        try {
            serverSocket.bind(socketAddress);
            return serverSocket;
        } catch (final Exception e) {
            SocketUtils.close(serverSocket, e);
            throw e;
        }
    }

    public SimpleSocketBinder(final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        this.socketFactory = Check.notNull(socketFactory);
        this.socketAddress = Check.notNull(socketAddress);
    }

    public SimpleSocketBinder(final SocketAddress socketAddress) {
        this(ServerSocketFactory.getDefault(), socketAddress);
    }

}
