package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executor;

public abstract class AbstractSocketTransport {

    private final Executor readerExecutor;

    AbstractSocketTransport(final Executor readerExecutor) {
        this.readerExecutor = Check.notNull(readerExecutor);
    }

    @FunctionalInterface interface SocketAction {
        void action(Socket socket) throws Exception;
    }

    final void runInReaderExecutor(final Socket socket, final SocketAction socketAction) {
        try {
            readerExecutor.execute(() -> {
                try {
                    setForceImmediateSend(socket);
                    socketAction.action(socket);
                } catch (final Exception e) {
                    close(socket, e);
                    throw Exceptions.wrap(e);
                }
            });
        } catch (final Exception e) {
            close(socket, e);
            throw e;
        }
    }

    public static final class ListenerCloser implements AutoCloseable {
        private final ServerSocket serverSocket;
        ListenerCloser(final ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }
        /**
         * This method is idempotent.
         */
        @Override public void close() {
            try {
                serverSocket.close();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @param listenerExecutor used once
     */
    final ListenerCloser start(final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress, final SocketAction socketAction) {
        Check.notNull(socketAction);
        try {
            final ServerSocket serverSocket = socketFactory.createServerSocket();
            try {
                serverSocket.bind(socketAddress);
                listenerExecutor.execute(() -> {
                    try {
                        while (true) {
                            runInReaderExecutor(serverSocket.accept(), socketAction);
                        }
                    } catch (final Exception e) {
                        if (serverSocket.isClosed()) {
                            return;
                        }
                        close(serverSocket, e);
                        throw Exceptions.wrap(e);
                    }
                });
            } catch (final Exception e) {
                close(serverSocket, e);
                throw e;
            }
            return new ListenerCloser(serverSocket);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Socket connect(final SocketFactory socketFactory, final SocketAddress socketAddress) throws IOException {
        final Socket socket = socketFactory.createSocket();
        try {
            socket.connect(socketAddress);
            return socket;
        } catch (final Exception e) {
            close(socket, e);
            throw e;
        }
    }

    static void setForceImmediateSend(final Socket socket) throws SocketException {
        socket.setTcpNoDelay(true);
    }

    static void close(final Socket socket, final Exception e) {
        try {
            socket.close();
        } catch (final Exception e2) {
            e.addSuppressed(e2);
        }
    }

    static void close(final ServerSocket serverSocket, final Exception e) {
        try {
            serverSocket.close();
        } catch (final Exception e2) {
            e.addSuppressed(e2);
        }
    }

}
