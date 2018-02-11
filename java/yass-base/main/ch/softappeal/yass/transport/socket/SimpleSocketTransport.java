package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.PathSerializer;
import ch.softappeal.yass.transport.SimplePathResolver;
import ch.softappeal.yass.transport.SimpleTransportSetup;
import ch.softappeal.yass.util.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Objects;
import java.util.Optional;
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
        this.pathSerializer = Objects.requireNonNull(pathSerializer);
        this.pathResolver = Objects.requireNonNull(pathResolver);
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
        final var out = socket.getOutputStream();
        buffer.writeTo(out);
        out.flush();
    }

    @Override void accept(final Socket socket) throws Exception {
        try (socket) {
            final var oldSocket = SOCKET.get();
            SOCKET.set(socket);
            try {
                final var reader = Reader.create(socket.getInputStream());
                final var setup = pathResolver.resolvePath(pathSerializer.read(reader));
                setup.server.invocation(false, (Request)setup.messageSerializer.read(reader)).invoke(reply -> {
                    final var buffer = createBuffer();
                    setup.messageSerializer.write(reply, Writer.create(buffer));
                    flush(buffer, socket);
                });
            } finally {
                SOCKET.set(oldSocket);
            }
        }
    }

    public static Client client(
        final Serializer messageSerializer, final SocketConnector socketConnector,
        final Serializer pathSerializer, final Object path
    ) {
        Objects.requireNonNull(messageSerializer);
        Objects.requireNonNull(socketConnector);
        Objects.requireNonNull(pathSerializer);
        Objects.requireNonNull(path);
        return new Client() {
            @Override protected Object invokeSync(final ContractId<?> contractId, final Interceptor interceptor, final Method method, final @Nullable Object[] arguments) throws Exception {
                try (var socket = socketConnector.get()) {
                    SocketUtils.setForceImmediateSend(socket);
                    final var oldSocket = SOCKET.get();
                    SOCKET.set(socket);
                    try {
                        return super.invokeSync(contractId, interceptor, method, arguments);
                    } finally {
                        SOCKET.set(oldSocket);
                    }
                }
            }
            @Override public void invoke(final Client.Invocation invocation) throws Exception {
                invocation.invoke(false, request -> {
                    final var buffer = createBuffer();
                    final var writer = Writer.create(buffer);
                    pathSerializer.write(path, writer);
                    messageSerializer.write(request, writer);
                    final var socket = SOCKET.get();
                    flush(buffer, socket);
                    invocation.settle((Reply)messageSerializer.read(Reader.create(socket.getInputStream())));
                });
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
    public static Optional<Socket> socket() {
        return Optional.ofNullable(SOCKET.get());
    }

}
