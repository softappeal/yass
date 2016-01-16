package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.PathSerializer;
import ch.softappeal.yass.transport.SimplePathResolver;
import ch.softappeal.yass.transport.SimpleTransportSetup;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Nullable;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

/**
 * Each request gets its own socket.
 */
public final class SimpleSocketTransport extends AbstractSocketTransport {

    /**
     * @param requestExecutor used once for each request
     */
    public SimpleSocketTransport(final Executor requestExecutor) {
        super(requestExecutor);
    }

    /**
     * Buffering of output is needed to prevent long delays due to Nagle's algorithm.
     */
    private static ByteArrayOutputStream createBuffer() {
        return new ByteArrayOutputStream(128);
    }

    private static void flush(final ByteArrayOutputStream buffer, final Socket socket) throws IOException {
        final OutputStream out = socket.getOutputStream();
        buffer.writeTo(out);
        out.flush();
    }

    /**
     * @param listenerExecutor used once
     * @return closer for socket listener
     */
    @SuppressWarnings("try")
    public Closer start(
        final SimplePathResolver pathResolver, final Serializer pathSerializer,
        final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress
    ) {
        Check.notNull(pathResolver);
        Check.notNull(pathSerializer);
        return start(listenerExecutor, socketFactory, socketAddress, socket -> {
            try (Socket closer = socket) {
                final @Nullable Socket oldSocket = SOCKET.get();
                SOCKET.set(socket);
                try {
                    final Reader reader = Reader.create(socket.getInputStream());
                    final SimpleTransportSetup transportSetup = pathResolver.resolvePath(pathSerializer.read(reader));
                    final Server.Invocation invocation = transportSetup.server.invocation((Request)transportSetup.messageSerializer.read(reader));
                    final Reply reply = invocation.invoke();
                    if (!invocation.methodMapping.oneWay) {
                        final ByteArrayOutputStream buffer = createBuffer();
                        transportSetup.messageSerializer.write(reply, Writer.create(buffer));
                        flush(buffer, socket);
                    }
                } finally {
                    SOCKET.set(oldSocket);
                }
            }
        });
    }

    public Closer start(
        final Serializer messageSerializer, final Server server,
        final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress
    ) {
        return start(
            new SimplePathResolver(PathSerializer.DEFAULT, new SimpleTransportSetup(messageSerializer, server)), PathSerializer.INSTANCE,
            listenerExecutor, socketFactory, socketAddress
        );
    }

    public Closer start(
        final Serializer messageSerializer, final Server server,
        final Executor listenerExecutor, final SocketAddress socketAddress
    ) {
        return start(
            messageSerializer, server,
            listenerExecutor, ServerSocketFactory.getDefault(), socketAddress
        );
    }

    /**
     * @param connectTimeoutMilliSeconds see {@link Socket#connect(SocketAddress, int)}
     * @param readTimeoutMilliSeconds see {@link Socket#setSoTimeout(int)}
     */
    public static Client client(
        final Serializer pathSerializer, final Object path, final Serializer messageSerializer,
        final SocketFactory socketFactory, final SocketAddress socketAddress, final int connectTimeoutMilliSeconds, final int readTimeoutMilliSeconds
    ) {
        Check.notNull(pathSerializer);
        Check.notNull(path);
        Check.notNull(messageSerializer);
        Check.notNull(socketFactory);
        Check.notNull(socketAddress);
        if (connectTimeoutMilliSeconds < 0) {
            throw new IllegalArgumentException("connectTimeoutMilliSeconds < 0");
        }
        if (readTimeoutMilliSeconds < 0) {
            throw new IllegalArgumentException("readTimeoutMilliSeconds < 0");
        }
        return new Client() {
            @Override public Object invoke(final Client.Invocation invocation) throws Exception {
                try (Socket socket = connect(socketFactory, socketAddress, connectTimeoutMilliSeconds)) {
                    setForceImmediateSend(socket);
                    socket.setSoTimeout(readTimeoutMilliSeconds);
                    final @Nullable Socket oldSocket = SOCKET.get();
                    SOCKET.set(socket);
                    try {
                        return invocation.invoke(request -> {
                            final ByteArrayOutputStream buffer = createBuffer();
                            final Writer writer = Writer.create(buffer);
                            pathSerializer.write(path, writer);
                            messageSerializer.write(request, writer);
                            flush(buffer, socket);
                            return invocation.methodMapping.oneWay ? null : (Reply)messageSerializer.read(Reader.create(socket.getInputStream()));
                        });
                    } finally {
                        SOCKET.set(oldSocket);
                    }
                }
            }
        };
    }

    public static Client client(
        final Serializer messageSerializer,
        final SocketFactory socketFactory, final SocketAddress socketAddress, final int connectTimeoutMilliSeconds, final int readTimeoutMilliSeconds
    ) {
        return client(
            PathSerializer.INSTANCE, PathSerializer.DEFAULT, messageSerializer,
            socketFactory, socketAddress, connectTimeoutMilliSeconds, readTimeoutMilliSeconds
        );
    }

    public static Client client(
        final Serializer messageSerializer,
        final SocketAddress socketAddress, final int connectTimeoutMilliSeconds, final int readTimeoutMilliSeconds
    ) {
        return client(
            messageSerializer,
            SocketFactory.getDefault(), socketAddress, connectTimeoutMilliSeconds, readTimeoutMilliSeconds
        );
    }

    private static final ThreadLocal<Socket> SOCKET = new ThreadLocal<>();
    /**
     * @return socket of current request (if any)
     */
    public static @Nullable Socket socket() {
        return SOCKET.get();
    }

}
