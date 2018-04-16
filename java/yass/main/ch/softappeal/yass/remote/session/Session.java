package ch.softappeal.yass.remote.session;

import ch.softappeal.yass.Closer;
import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.remote.Client;
import ch.softappeal.yass.remote.ExceptionReply;
import ch.softappeal.yass.remote.Reply;
import ch.softappeal.yass.remote.Request;
import ch.softappeal.yass.remote.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Session extends Client implements Closer {

    public final Connection connection;

    protected Session(final Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    /**
     * Called if a session has been opened.
     * Must call {@link Runnable#run()} (possibly in an own thread).
     */
    protected abstract void dispatchOpened(Runnable runnable) throws Exception;

    /**
     * Called for an incoming request.
     * Must call {@link Runnable#run()} (possibly in an own thread).
     */
    protected abstract void dispatchServerInvoke(Server.Invocation invocation, Runnable runnable) throws Exception;

    private Server server;
    /**
     * Gets the server of this session. Called only once after creation of session.
     * This implementation returns {@link Server#EMPTY}.
     */
    protected Server server() throws Exception {
        return Server.EMPTY;
    }

    /**
     * Called when the session has been opened.
     * This implementation does nothing.
     * Due to race conditions or exceptions it could not be called or be called after {@link #closed(Exception)}.
     * @see SessionFactory#create(Connection)
     */
    protected void opened() throws Exception {
        // empty
    }

    /**
     * Called once when the session has been closed.
     * This implementation does nothing.
     * @param exception if (exception == null) regular close else reason for close
     * @see SessionFactory#create(Connection)
     */
    protected void closed(final @Nullable Exception exception) throws Exception {
        // empty
    }

    private final AtomicBoolean closed = new AtomicBoolean(true);
    public final boolean isClosed() {
        return closed.get();
    }

    private void unblockPromises() {
        for (final var invocation : new ArrayList<>(requestNumber2invocation.values())) {
            try {
                invocation.settle(new ExceptionReply(new SessionClosedException()));
            } catch (final Exception ignore) {
                // empty
            }
        }
    }

    private void close(final boolean sendEnd, final @Nullable Exception exception) {
        if (closed.getAndSet(true)) {
            return;
        }
        try {
            try {
                try {
                    unblockPromises();
                } finally {
                    closed(exception);
                }
                if (sendEnd) {
                    connection.write(Packet.END);
                }
            } finally {
                connection.closed();
            }
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    /**
     * Must be called if communication has failed.
     * This method is idempotent.
     */
    public static void close(final Session session, final Exception e) {
        session.close(false, Objects.requireNonNull(e));
    }

    private void closeThrow(final Exception e) throws Exception {
        try {
            close(this, e);
        } catch (final Exception e2) {
            e.addSuppressed(e2);
        }
        throw e;
    }

    /**
     * @see #close(Session, Exception)
     */
    public static void closeThrow(final Session session, final Exception e) throws Exception {
        session.closeThrow(e);
    }

    @Override public void close() {
        close(true, null);
    }

    private void serverInvoke(final int requestNumber, final Request request) throws Exception {
        final var invocation = server.invocation(true, request);
        dispatchServerInvoke(invocation, () -> {
            try {
                invocation.invoke(reply -> {
                    if (!invocation.methodMapping.oneWay) {
                        try {
                            connection.write(new Packet(requestNumber, reply));
                        } catch (final Exception e) {
                            closeThrow(e);
                        }
                    }
                });
            } catch (final Exception e) {
                close(this, e);
            }
        });
    }

    /**
     * note: it's not worth to use {@link ConcurrentHashMap} here
     */
    private final Map<Integer, Invocation> requestNumber2invocation = Collections.synchronizedMap(new HashMap<>(16));

    private void received(final Packet packet) throws Exception {
        try {
            if (packet.isEnd()) {
                close(false, null);
                return;
            }
            final var message = packet.message();
            if (message instanceof Request) {
                serverInvoke(packet.requestNumber(), (Request)message);
            } else {
                requestNumber2invocation.remove(packet.requestNumber()).settle((Reply)message); // client invoke
            }
        } catch (final Exception e) {
            closeThrow(e);
        }
    }

    /**
     * Must be called if a packet has been received.
     * It must also be called if {@link Packet#isEnd()}; however, it must not be called again after that.
     */
    public static void received(final Session session, final Packet packet) throws Exception {
        session.received(packet);
    }

    private final AtomicInteger nextRequestNumber = new AtomicInteger(Packet.END_REQUEST_NUMBER);

    @Override protected final void invoke(final Client.Invocation invocation) throws Exception {
        if (isClosed()) {
            throw new SessionClosedException();
        }
        invocation.invoke(true, request -> {
            try {
                int requestNumber;
                do { // we can't use END_REQUEST_NUMBER as regular requestNumber
                    requestNumber = nextRequestNumber.incrementAndGet();
                } while (requestNumber == Packet.END_REQUEST_NUMBER);
                if (!invocation.methodMapping.oneWay) {
                    requestNumber2invocation.put(requestNumber, invocation);
                    if (isClosed()) {
                        unblockPromises(); // needed due to race conditions
                    }
                }
                connection.write(new Packet(requestNumber, request));
            } catch (final Exception e) {
                closeThrow(e);
            }
        });
    }

    private void created() throws Exception {
        closed.set(false);
        try {
            server = Objects.requireNonNull(server());
            dispatchOpened(() -> {
                try {
                    opened();
                } catch (final Exception e) {
                    close(this, e);
                }
            });
        } catch (final Exception e) {
            closeThrow(e);
        }
    }

    public static Session create(final SessionFactory sessionFactory, final Connection connection) throws Exception {
        final var session = sessionFactory.create(connection);
        session.created();
        return session;
    }

}
