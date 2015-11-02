package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.PathSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public final class SocketTransport {

    public final Executor readerExecutor;
    public final SocketConnection.Factory connectionFactory;
    public final Serializer pathSerializer;

    /**
     * @param readerExecutor used once for each session
     */
    public SocketTransport(final Executor readerExecutor, final SocketConnection.Factory connectionFactory, final Serializer pathSerializer) {
        this.readerExecutor = Check.notNull(readerExecutor);
        this.connectionFactory = Check.notNull(connectionFactory);
        this.pathSerializer = Check.notNull(pathSerializer);
    }

    /**
     * Uses {@link PathSerializer}.
     */
    public SocketTransport(final Executor readerExecutor, final SocketConnection.Factory connectionFactory) {
        this(readerExecutor, connectionFactory, PathSerializer.INSTANCE);
    }

    @FunctionalInterface private interface Action {
        void action() throws Exception;
    }

    private void runInReaderExecutor(final Socket socket, final Action action) {
        try {
            readerExecutor.execute(() -> {
                try {
                    socket.setTcpNoDelay(true); // forces immediate send
                    action.action();
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

    public void connect(final TransportSetup setup, final Object path, final SocketFactory socketFactory, final SocketAddress socketAddress) {
        Check.notNull(setup);
        Check.notNull(path);
        final Socket socket;
        try {
            socket = socketFactory.createSocket();
            try {
                socket.connect(socketAddress);
            } catch (final Exception e) {
                close(socket, e);
                throw e;
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        runInReaderExecutor(socket, () -> {
            final OutputStream out = socket.getOutputStream();
            pathSerializer.write(path, Writer.create(out));
            out.flush();
            SocketConnection.create(connectionFactory, setup, socket, Reader.create(socket.getInputStream()), out);
        });
    }

    /**
     * Uses {@link PathSerializer#DEFAULT}.
     */
    public void connect(final TransportSetup setup, final SocketFactory socketFactory, final SocketAddress socketAddress) {
        connect(setup, PathSerializer.DEFAULT, socketFactory, socketAddress);
    }

    /**
     * Uses {@link SocketFactory#getDefault()} and {@link PathSerializer#DEFAULT}.
     */
    public void connect(final TransportSetup setup, final SocketAddress socketAddress) {
        connect(setup, SocketFactory.getDefault(), socketAddress);
    }

    static final int ACCEPT_TIMEOUT_MILLISECONDS = 200;

    /**
     * @param listenerExecutor used once, must interrupt it's threads to terminate the socket listener (use {@link ExecutorService#shutdownNow()})
     */
    public void start(final PathResolver pathResolver, final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        Check.notNull(pathResolver);
        try {
            final ServerSocket serverSocket = socketFactory.createServerSocket();
            try {
                serverSocket.bind(socketAddress);
                serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MILLISECONDS);
                listenerExecutor.execute(new Runnable() {
                    void accept() throws IOException {
                        while (!Thread.interrupted()) {
                            final Socket socket;
                            try {
                                socket = serverSocket.accept();
                            } catch (final SocketTimeoutException ignore) { // thrown if SoTimeout reached
                                continue;
                            } catch (final InterruptedIOException ignore) {
                                return; // needed because some VM's (for example: Sun Solaris) throw this exception if the thread gets interrupted
                            }
                            runInReaderExecutor(socket, () -> {
                                final Reader reader = Reader.create(socket.getInputStream());
                                final TransportSetup setup = pathResolver.resolvePath(pathSerializer.read(reader));
                                SocketConnection.create(connectionFactory, setup, socket, reader, socket.getOutputStream());
                            });
                        }
                    }
                    @Override public void run() {
                        try {
                            try {
                                accept();
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
     * Uses {@link PathSerializer#DEFAULT}.
     */
    public void start(final TransportSetup setup, final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        start(new PathResolver(PathSerializer.DEFAULT, setup), listenerExecutor, socketFactory, socketAddress);
    }

    /**
     * Uses {@link ServerSocketFactory#getDefault()} and {@link PathSerializer#DEFAULT}.
     */
    public void start(final TransportSetup setup, final Executor listenerExecutor, final SocketAddress socketAddress) {
        start(setup, listenerExecutor, ServerSocketFactory.getDefault(), socketAddress);
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
