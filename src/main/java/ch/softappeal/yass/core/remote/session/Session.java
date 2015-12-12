package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Session extends Client implements AutoCloseable {

    public static Session create(final SessionFactory sessionFactory, final Connection connection) throws Exception {
        final Session session = sessionFactory.create(connection);
        session.created();
        return session;
    }

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
     */
    protected abstract Server server();

    /**
     * Called when the session has been opened.
     * This implementation does nothing.
     */
    protected void opened() throws Exception {
        // empty
    }

    /**
     * Called when the session has been closed.
     * This implementation does nothing.
     * @param exceptional true if exceptional close else regular close
     */
    protected void closed(final boolean exceptional) throws Exception {
        // empty
    }

    private final AtomicBoolean closed = new AtomicBoolean(true);

    private void created() throws Exception {
        server = Check.notNull(server());
        closed.set(false);
        dispatchOpened(() -> {
            try {
                opened();
            } catch (final Exception e) {
                throw Exceptions.wrap(e);
            }
        });
    }

    /**
     * Must be called if communication has failed.
     * This method is idempotent.
     */
    public static void close(final Session session, final Exception exception) {
        try {
            session.close(false, true);
        } catch (final Exception e2) {
            exception.addSuppressed(e2);
        }
    }

    public final boolean isClosed() {
        return closed.get();
    }

    private void close(final boolean sendEnd, final boolean exceptional) throws Exception {
        if (closed.getAndSet(true)) {
            return;
        }
        try {
            closed(exceptional);
            if (sendEnd) {
                connection.write(Packet.END);
            }
        } catch (final Exception e) {
            try {
                connection.closed();
            } catch (final Exception e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }
        connection.closed();
    }

    /**
     * Closes the session.
     * This method is idempotent.
     */
    @Override public void close() {
        try {
            close(true, false);
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    private void write(final int requestNumber, final Message message) throws Exception {
        try {
            connection.write(new Packet(requestNumber, message));
        } catch (final Exception e) {
            close(this, e);
            throw e;
        }
    }

    private void serverInvoke(final int requestNumber, final Request request) throws Exception {
        final Server.Invocation invocation = server.invocation(request);
        dispatchServerInvoke(invocation, () -> {
            try {
                final Reply reply = invocation.invoke();
                if (!invocation.methodMapping.oneWay) {
                    write(requestNumber, reply);
                }
            } catch (final Exception e) {
                throw Exceptions.wrap(e);
            }
        });
    }

    private final Map<Integer, BlockingQueue<Reply>> requestNumber2replyQueue = Collections.synchronizedMap(new HashMap<>(16));

    private void received(final Packet packet) throws Exception {
        if (packet.isEnd()) {
            close(false, false);
            return;
        }
        final Message message = packet.message();
        if (message instanceof Request) {
            serverInvoke(packet.requestNumber(), (Request)message);
        } else {
            requestNumber2replyQueue.remove(packet.requestNumber()).put((Reply)message); // client invoke
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
            int requestNumber;
            do { // we can't use END_REQUEST_NUMBER as regular requestNumber
                requestNumber = nextRequestNumber.incrementAndGet();
            } while (requestNumber == Packet.END_REQUEST_NUMBER);
            if (invocation.methodMapping.oneWay) {
                write(requestNumber, request);
                return null;
            }
            final BlockingQueue<Reply> replyQueue = new ArrayBlockingQueue<>(1, false); // we use unfair for speed
            if (requestNumber2replyQueue.put(requestNumber, replyQueue) != null) {
                throw new RuntimeException("already waiting for requestNumber " + requestNumber);
            }
            write(requestNumber, request);
            while (true) {
                final Reply reply = replyQueue.poll(200L, TimeUnit.MILLISECONDS);
                if (reply != null) {
                    return reply;
                } else if (isClosed()) {
                    throw new SessionClosedException();
                }
            }
        });
    }

}
