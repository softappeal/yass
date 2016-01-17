package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Exceptions;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

public abstract class SocketListener {

    private final Executor acceptExecutor;

    /**
     * @param acceptExecutor used once for each accept
     */
    SocketListener(final Executor acceptExecutor) {
        this.acceptExecutor = Check.notNull(acceptExecutor);
    }

    abstract void accept(Socket socket) throws Exception;

    /**
     * @param listenerExecutor used once
     * @return closer for socket listener
     */
    public final Closer start(final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        try {
            final ServerSocket serverSocket = socketFactory.createServerSocket();
            try {
                serverSocket.bind(socketAddress);
                listenerExecutor.execute(() -> {
                    try {
                        while (true) {
                            SocketUtils.execute(acceptExecutor, serverSocket.accept(), this::accept);
                        }
                    } catch (final Exception e) {
                        if (serverSocket.isClosed()) {
                            return;
                        }
                        SocketUtils.close(serverSocket, e);
                        throw Exceptions.wrap(e);
                    }
                });
            } catch (final Exception e) {
                SocketUtils.close(serverSocket, e);
                throw e;
            }
            return () -> {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final Closer start(final Executor listenerExecutor, final SocketAddress socketAddress) {
        return start(listenerExecutor, ServerSocketFactory.getDefault(), socketAddress);
    }

}
