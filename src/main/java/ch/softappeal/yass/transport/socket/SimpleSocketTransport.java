package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Nullable;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.ByteArrayOutputStream;
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
     * @param listenerExecutor used once
     * @return closer for socket listener
     */
    @SuppressWarnings("try")
    public Closer start(final Server server, final Serializer messageSerializer, final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        Check.notNull(server);
        Check.notNull(messageSerializer);
        return start(listenerExecutor, socketFactory, socketAddress, socket -> {
            try (Socket closer = socket) {
                final @Nullable Socket oldSocket = SOCKET.get();
                SOCKET.set(socket);
                try {
                    final Server.Invocation invocation = server.invocation((Request)messageSerializer.read(Reader.create(socket.getInputStream())));
                    final Reply reply = invocation.invoke();
                    if (!invocation.methodMapping.oneWay) {
                        write(reply, socket, messageSerializer);
                    }
                } finally {
                    SOCKET.set(oldSocket);
                }
            }
        });
    }

    public Closer start(final Server server, final Serializer messageSerializer, final Executor listenerExecutor, final SocketAddress socketAddress) {
        return start(server, messageSerializer, listenerExecutor, ServerSocketFactory.getDefault(), socketAddress);
    }

    /**
     * Buffering of output is needed to prevent long delays due to Nagle's algorithm.
     */
    private static void write(final Message message, final Socket socket, final Serializer messageSerializer) throws Exception {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(128);
        messageSerializer.write(message, Writer.create(buffer));
        final OutputStream out = socket.getOutputStream();
        buffer.writeTo(out);
        out.flush();
    }

    public static Client client(final Serializer messageSerializer, final SocketFactory socketFactory, final SocketAddress socketAddress) {
        Check.notNull(messageSerializer);
        Check.notNull(socketFactory);
        Check.notNull(socketAddress);
        return new Client() {
            @Override public Object invoke(final Client.Invocation invocation) throws Exception {
                try (Socket socket = connect(socketFactory, socketAddress)) {
                    setForceImmediateSend(socket);
                    final @Nullable Socket oldSocket = SOCKET.get();
                    SOCKET.set(socket);
                    try {
                        return invocation.invoke(request -> {
                            write(request, socket, messageSerializer);
                            return invocation.methodMapping.oneWay ? null : (Reply)messageSerializer.read(Reader.create(socket.getInputStream()));
                        });
                    } finally {
                        SOCKET.set(oldSocket);
                    }
                }
            }
        };
    }

    public static Client client(final Serializer messageSerializer, final SocketAddress socketAddress) {
        return client(messageSerializer, SocketFactory.getDefault(), socketAddress);
    }

    private static final ThreadLocal<Socket> SOCKET = new ThreadLocal<>();
    /**
     * @return socket of current request (if any)
     */
    public static @Nullable Socket socket() {
        return SOCKET.get();
    }

}
