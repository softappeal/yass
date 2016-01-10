package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Exceptions;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executor;

abstract class AbstractSocketTransport {

    private final Executor acceptExecutor;

    /**
     * @param acceptExecutor used once for each accept
     */
    AbstractSocketTransport(final Executor acceptExecutor) {
        this.acceptExecutor = Check.notNull(acceptExecutor);
    }

    interface SocketAction {
        void action(Socket socket) throws Exception;
    }

    final void runInAcceptExecutor(final Socket socket, final SocketAction socketAction) {
        try {
            acceptExecutor.execute(new Runnable() {
                @Override public void run() {
                    try {
                        setForceImmediateSend(socket);
                        socketAction.action(socket);
                    } catch (final Exception e) {
                        close(socket, e);
                        throw Exceptions.wrap(e);
                    }
                }
            });
        } catch (final Exception e) {
            close(socket, e);
            throw e;
        }
    }

    /**
     * @param listenerExecutor used once
     * @return closer for socket listener
     */
    final Closer start(final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress, final SocketAction socketAction) {
        Check.notNull(socketAction);
        try {
            final ServerSocket serverSocket = socketFactory.createServerSocket();
            try {
                serverSocket.bind(socketAddress);
                listenerExecutor.execute(new Runnable() {
                    @Override public void run() {
                        try {
                            while (true) {
                                AbstractSocketTransport.this.runInAcceptExecutor(serverSocket.accept(), socketAction);
                            }
                        } catch (final Exception e) {
                            if (serverSocket.isClosed()) {
                                return;
                            }
                            close(serverSocket, e);
                            throw Exceptions.wrap(e);
                        }
                    }
                });
            } catch (final Exception e) {
                close(serverSocket, e);
                throw e;
            }
            return new Closer() {
                @Override public void close() {
                    try {
                        serverSocket.close();
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
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
