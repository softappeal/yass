package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.PathSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;

import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executor;

/**
 * Each session gets its own socket.
 */
public final class SocketTransport extends SocketListener {

    private final SocketConnection.Factory connectionFactory;
    private final Serializer pathSerializer;
    private final PathResolver pathResolver;

    /**
     * @param readerExecutor used once for each session
     */
    public SocketTransport(
        final Executor readerExecutor, final SocketConnection.Factory connectionFactory,
        final Serializer pathSerializer, final PathResolver pathResolver
    ) {
        super(readerExecutor);
        this.connectionFactory = Check.notNull(connectionFactory);
        this.pathSerializer = Check.notNull(pathSerializer);
        this.pathResolver = Check.notNull(pathResolver);
    }

    public SocketTransport(
        final Executor readerExecutor, final SocketConnection.Factory connectionFactory,
        final TransportSetup setup
    ) {
        this(
            readerExecutor, connectionFactory,
            PathSerializer.INSTANCE, new PathResolver(PathSerializer.DEFAULT, setup)
        );
    }

    @Override void accept(final Socket socket) throws Exception {
        final Reader reader = Reader.create(socket.getInputStream());
        final TransportSetup setup = pathResolver.resolvePath(pathSerializer.read(reader));
        SocketConnection.create(connectionFactory, setup, socket, reader, socket.getOutputStream());
    }

    public static void connect(
        final Executor readerExecutor, final SocketConnection.Factory connectionFactory, final TransportSetup setup, final SocketConnector socketConnector,
        final Serializer pathSerializer, final Object path
    ) {
        Check.notNull(connectionFactory);
        Check.notNull(setup);
        Check.notNull(pathSerializer);
        Check.notNull(path);
        try {
            SocketUtils.execute(readerExecutor, socketConnector.connect(), socket -> {
                final OutputStream out = socket.getOutputStream();
                pathSerializer.write(path, Writer.create(out));
                out.flush();
                SocketConnection.create(connectionFactory, setup, socket, Reader.create(socket.getInputStream()), out);
            });
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    public static void connect(final Executor readerExecutor, final SocketConnection.Factory connectionFactory, final TransportSetup setup, final SocketConnector socketConnector) {
        connect(
            readerExecutor, connectionFactory, setup, socketConnector,
            PathSerializer.INSTANCE, PathSerializer.DEFAULT
        );
    }

}
