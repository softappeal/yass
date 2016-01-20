package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Tunnel;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.PathSerializer;
import ch.softappeal.yass.transport.SimplePathResolver;
import ch.softappeal.yass.transport.SimpleTransportSetup;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executor;

/**
 * Each request gets its own socket.
 */
public final class SimpleSocketTransport extends SocketListener {

    private final Serializer pathSerializer;
    private final SimplePathResolver pathResolver;

    /**
     * @param requestExecutor used once for each request
     */
    public SimpleSocketTransport(
        final Executor requestExecutor,
        final Serializer pathSerializer, final SimplePathResolver pathResolver
    ) {
        super(requestExecutor);
        this.pathSerializer = Check.notNull(pathSerializer);
        this.pathResolver = Check.notNull(pathResolver);
    }

    public SimpleSocketTransport(
        final Executor requestExecutor,
        final Serializer messageSerializer, final Server server
    ) {
        this(
            requestExecutor,
            PathSerializer.INSTANCE, new SimplePathResolver(PathSerializer.DEFAULT, new SimpleTransportSetup(messageSerializer, server))
        );
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

    @SuppressWarnings("try")
    @Override void accept(final Socket socket) throws Exception {
        try (Socket closer = socket) {
            final @Nullable Socket oldSocket = SOCKET.get();
            SOCKET.set(socket);
            try {
                final Reader reader = Reader.create(socket.getInputStream());
                final SimpleTransportSetup setup = pathResolver.resolvePath(pathSerializer.read(reader));
                final Server.Invocation invocation = setup.server.invocation((Request)setup.messageSerializer.read(reader));
                final Reply reply = invocation.invoke();
                if (!invocation.methodMapping.oneWay) {
                    final ByteArrayOutputStream buffer = createBuffer();
                    setup.messageSerializer.write(reply, Writer.create(buffer));
                    flush(buffer, socket);
                }
            } finally {
                SOCKET.set(oldSocket);
            }
        }
    }

    public static Client client(
        final Serializer messageSerializer, final SocketConnector socketConnector,
        final Serializer pathSerializer, final Object path
    ) {
        Check.notNull(messageSerializer);
        Check.notNull(socketConnector);
        Check.notNull(pathSerializer);
        Check.notNull(path);
        return new Client() {
            @Override public Object invoke(final Client.Invocation invocation) throws Exception {
                try (Socket socket = socketConnector.connect()) {
                    SocketUtils.setForceImmediateSend(socket);
                    final @Nullable Socket oldSocket = SOCKET.get();
                    SOCKET.set(socket);
                    try {
                        return invocation.invoke(new Tunnel() {
                            @Override public Reply invoke(final Request request) throws Exception {
                                final ByteArrayOutputStream buffer = createBuffer();
                                final Writer writer = Writer.create(buffer);
                                pathSerializer.write(path, writer);
                                messageSerializer.write(request, writer);
                                flush(buffer, socket);
                                return invocation.methodMapping.oneWay ? null : (Reply)messageSerializer.read(Reader.create(socket.getInputStream()));
                            }
                        });
                    } finally {
                        SOCKET.set(oldSocket);
                    }
                }
            }
        };
    }

    public static Client client(final Serializer messageSerializer, final SocketConnector socketConnector) {
        return client(
            messageSerializer, socketConnector,
            PathSerializer.INSTANCE, PathSerializer.DEFAULT
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
