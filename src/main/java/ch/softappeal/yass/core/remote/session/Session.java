package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Session extends Client implements Closer {

    public final Connection connection;

    protected Session(final Connection connection) {
        this.connection = Check.notNull(connection);
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
     * @throws Exception note: will be ignored
     * @see SessionFactory#create(Connection)
     */
    protected void closed(final @Nullable Exception exception) throws Exception {
        // empty
    }

    private final AtomicBoolean closed = new AtomicBoolean(true);
    public final boolean isClosed() {
        return closed.get();
    }

    private void close(final boolean sendEnd, final @Nullable Exception exception) {
        if (closed.getAndSet(true)) {
            return;
        }
        try {
            try {
                closed(exception);
                if (sendEnd) {
                    connection.write(Packet.END);
                }
            } finally {
                connection.closed();
            }
        } catch (final Exception ignore) {
            // empty
        }
    }

    /**
     * Must be called if communication has failed.
     * This method is idempotent.
     */
    public static void close(final Session session, final Exception e) {
        session.close(false, Check.notNull(e));
    }

    @Override public void close() {
        close(true, null);
    }

    private void serverInvoke(final int requestNumber, final Request request) throws Exception {
        final Server.Invocation invocation = server.invocation(request);
        dispatchServerInvoke(invocation, () -> {
            try {
                final Reply reply = invocation.invoke();
                if (!invocation.methodMapping.oneWay) {
                    connection.write(new Packet(requestNumber, reply));
                }
            } catch (final Exception ignore) {
                close(this, ignore);
            }
        });
    }

    /**
     * note: it's not worth to use {@link ConcurrentHashMap} here
     */
    private final Map<Integer, BlockingQueue<Reply>> requestNumber2replyQueue = Collections.synchronizedMap(new HashMap<>(16));

    private void received(final Packet packet) throws Exception {
        try {
            if (packet.isEnd()) {
                close(false, null);
                return;
            }
            final Message message = packet.message();
            if (message instanceof Request) {
                serverInvoke(packet.requestNumber(), (Request)message);
            } else {
                requestNumber2replyQueue.remove(packet.requestNumber()).put((Reply)message); // client invoke
            }
        } catch (final Exception e) {
            close(this, e);
            throw e;
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

    @Override protected final Object invoke(final Client.Invocation invocation) throws Exception {
        if (isClosed()) {
            throw new SessionClosedException();
        }
        return invocation.invoke(request -> {
            try {
                int requestNumber;
                do { // we can't use END_REQUEST_NUMBER as regular requestNumber
                    requestNumber = nextRequestNumber.incrementAndGet();
                } while (requestNumber == Packet.END_REQUEST_NUMBER);
                if (invocation.methodMapping.oneWay) {
                    connection.write(new Packet(requestNumber, request));
                    return null;
                }
                final BlockingQueue<Reply> replyQueue = new ArrayBlockingQueue<>(1, false); // we use unfair for speed
                if (requestNumber2replyQueue.put(requestNumber, replyQueue) != null) {
                    throw new RuntimeException("already waiting for requestNumber " + requestNumber);
                }
                connection.write(new Packet(requestNumber, request));
                while (true) {
                    final Reply reply = replyQueue.poll(1L, TimeUnit.SECONDS);
                    if (reply != null) {
                        return reply;
                    } else if (isClosed()) {
                        throw new SessionClosedException();
                    }
                }
            } catch (final Exception e) {
                close(this, e);
                throw e;
            }
        });
    }

    private void created() {
        closed.set(false);
        try {
            server = Check.notNull(server());
            dispatchOpened(() -> {
                try {
                    opened();
                } catch (final Exception ignore) {
                    close(this, ignore);
                }
            });
        } catch (final Exception ignore) {
            close(this, ignore);
        }
    }

    public static Session create(final SessionFactory sessionFactory, final Connection connection) throws Exception {
        final Session session = sessionFactory.create(connection);
        session.created();
        return session;
    }

}
