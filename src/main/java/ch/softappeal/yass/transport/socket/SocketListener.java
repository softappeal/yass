package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Exceptions;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
    public final Closer start(final Executor listenerExecutor, final SocketBinder socketBinder) {
        try {
            final ServerSocket serverSocket = socketBinder.bind();
            try {
                listenerExecutor.execute(new Runnable() {
                    @Override public void run() {
                        try {
                            while (true) {
                                SocketUtils.execute(acceptExecutor, serverSocket.accept(), new SocketUtils.SocketExecutor() {
                                    @Override public void execute(final Socket socket) throws Exception {
                                        SocketListener.this.accept(socket);
                                    }
                                });
                            }
                        } catch (final Exception e) {
                            if (serverSocket.isClosed()) {
                                return;
                            }
                            SocketUtils.close(serverSocket, e);
                            throw Exceptions.wrap(e);
                        }
                    }
                });
            } catch (final Exception e) {
                SocketUtils.close(serverSocket, e);
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
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

}
