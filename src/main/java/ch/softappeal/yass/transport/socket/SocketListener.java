package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public abstract class SocketListener {

    abstract void accept(Socket socket, Executor writerExecutor) throws Exception;

    static final int ACCEPT_TIMEOUT_MILLISECONDS = 100;

    /**
     * Starts a socket listener.
     * @param listenerExecutor must interrupt it's threads to terminate the socket listener (use {@link ExecutorService#shutdownNow()})
     */
    public final void start(
        final Executor listenerExecutor, final SocketExecutor socketExecutor,
        final ServerSocketFactory socketFactory, final SocketAddress socketAddress
    ) {
        Check.notNull(socketExecutor);
        try {
            final ServerSocket serverSocket = socketFactory.createServerSocket();
            try {
                serverSocket.bind(socketAddress);
                serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MILLISECONDS);
                listenerExecutor.execute(new Runnable() {

                    void loop() throws IOException {
                        while (true) {
                            if (Thread.interrupted()) {
                                break;
                            }
                            final Socket socket;
                            try {
                                socket = serverSocket.accept();
                            } catch (final SocketTimeoutException ignore) { // thrown if SoTimeout reached
                                continue;
                            } catch (final InterruptedIOException ignore) {
                                break; // needed because some VM's (for example: Sun Solaris) throw this exception if the thread gets interrupted
                            }
                            socketExecutor.execute(socket, SocketListener.this);
                        }
                    }

                    @Override public void run() {
                        try {
                            try {
                                loop();
                            } catch (final Exception e) {
                                close(serverSocket, e);
                                throw e;
                            }
                            serverSocket.close();
                        } catch (final IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                });
            } catch (final Exception e) {
                close(serverSocket, e);
                throw e;
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Uses {@link ServerSocketFactory#getDefault()}.
     */
    public final void start(
        final Executor listenerExecutor, final SocketExecutor socketExecutor,
        final SocketAddress socketAddress
    ) {
        start(listenerExecutor, socketExecutor, ServerSocketFactory.getDefault(), socketAddress);
    }

    static void close(final ServerSocket serverSocket, final Exception e) {
        try {
            serverSocket.close();
        } catch (final Exception e2) {
            e.addSuppressed(e2);
        }
    }

}
