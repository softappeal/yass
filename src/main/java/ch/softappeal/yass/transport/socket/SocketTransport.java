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
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

public final class SocketTransport {

    private final Executor readerExecutor;
    private final SocketConnection.Factory connectionFactory;
    private final Serializer pathSerializer;

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

    private interface Action {
        void action() throws Exception;
    }

    private void runInReaderExecutor(final Socket socket, final Action action) {
        try {
            readerExecutor.execute(new Runnable() {
                @Override public void run() {
                    try {
                        socket.setTcpNoDelay(true); // forces immediate send
                        action.action();
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
        runInReaderExecutor(socket, new Action() {
            @Override public void action() throws Exception {
                final OutputStream out = socket.getOutputStream();
                pathSerializer.write(path, Writer.create(out));
                out.flush();
                SocketConnection.create(connectionFactory, setup, socket, Reader.create(socket.getInputStream()), out);
            }
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

    public static final class ListenerCloser implements AutoCloseable {
        private final ServerSocket serverSocket;
        ListenerCloser(final ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }
        /**
         * This method is idempotent.
         */
        @Override public void close() {
            if (serverSocket.isClosed()) {
                return;
            }
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
    public ListenerCloser start(final PathResolver pathResolver, final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        Check.notNull(pathResolver);
        try {
            final ServerSocket serverSocket = socketFactory.createServerSocket();
            try {
                serverSocket.bind(socketAddress);
                listenerExecutor.execute(new Runnable() {
                    @Override public void run() {
                        try {
                            while (true) {
                                final Socket socket = serverSocket.accept();
                                SocketTransport.this.runInReaderExecutor(socket, new Action() {
                                    @Override public void action() throws Exception {
                                        final Reader reader = Reader.create(socket.getInputStream());
                                        final TransportSetup setup = pathResolver.resolvePath(pathSerializer.read(reader));
                                        SocketConnection.create(connectionFactory, setup, socket, reader, socket.getOutputStream());
                                    }
                                });
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
            return new ListenerCloser(serverSocket);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Uses {@link PathSerializer#DEFAULT}.
     */
    public ListenerCloser start(final TransportSetup setup, final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        return start(new PathResolver(PathSerializer.DEFAULT, setup), listenerExecutor, socketFactory, socketAddress);
    }

    /**
     * Uses {@link ServerSocketFactory#getDefault()} and {@link PathSerializer#DEFAULT}.
     */
    public ListenerCloser start(final TransportSetup setup, final Executor listenerExecutor, final SocketAddress socketAddress) {
        return start(setup, listenerExecutor, ServerSocketFactory.getDefault(), socketAddress);
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
