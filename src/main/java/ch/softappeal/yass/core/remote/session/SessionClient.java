package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Once a session is established, the communication is symmetrical between the two peers.
 * The only difference is that a client initiates a session and a server accepts sessions.
 */
public final class SessionClient extends Client {

    public final Connection connection;
    private final SessionSetup setup;
    final AtomicBoolean closed = new AtomicBoolean(false);
    private Session session = null;
    private Interceptor interceptor = null;

    private SessionClient(final SessionSetup setup, final Connection connection) {
        super(setup.server.methodMapperFactory);
        this.connection = Check.notNull(connection);
        this.setup = setup;
    }

    public static SessionClient create(final SessionSetup setup, final Connection connection) throws Exception {
        final SessionClient sessionClient = new SessionClient(setup, connection);
        sessionClient.session = Check.notNull(setup.sessionFactory.create(sessionClient));
        sessionClient.interceptor = Interceptor.threadLocal(Session.INSTANCE, sessionClient.session);
        setup.dispatcher.opened(sessionClient.session, () -> {
            try {
                sessionClient.session.opened();
            } catch (final Exception e) {
                sessionClient.close(e);
            }
        });
        return sessionClient;
    }

    private void close(final boolean sendEnd, @Nullable final Throwable throwable) {
        if (closed.getAndSet(true)) {
            return;
        }
        try {
            try {
                session.closed(throwable);
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
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    private void write(final int requestNumber, final Message message) throws SessionClosedException {
        if (closed.get()) {
            throw new SessionClosedException();
        }
        try {
            connection.write(new Packet(requestNumber, message));
        } catch (final Exception e) {
            close(e);
        }
    }

    private final Map<Integer, BlockingQueue<Reply>> requestNumber2replyQueue = Collections.synchronizedMap(new HashMap<>(16));

    private void replyReceived(final int requestNumber, final Reply reply) throws InterruptedException {
        @Nullable final BlockingQueue<Reply> replyQueue = requestNumber2replyQueue.remove(requestNumber);
        if (replyQueue != null) { // needed because request can be interrupted, see below
            replyQueue.put(reply);
        }
    }

    private RequestInterruptedException requestInterrupted(final int requestNumber) {
        requestNumber2replyQueue.remove(requestNumber);
        return new RequestInterruptedException();
    }

    private Reply clientRpcInvoke(final int requestNumber, final Request request) throws RequestInterruptedException, SessionClosedException {
        final BlockingQueue<Reply> replyQueue = new ArrayBlockingQueue<>(1, false); // we use unfair for speed
        if (requestNumber2replyQueue.put(requestNumber, replyQueue) != null) {
            throw new RuntimeException("already waiting for requestNumber " + requestNumber);
        }
        write(requestNumber, request);
        while (true) {
            if (Thread.interrupted()) {
                throw requestInterrupted(requestNumber);
            }
            try {
                final Reply reply = replyQueue.poll(200L, TimeUnit.MILLISECONDS);
                if (reply != null) {
                    return reply;
                } else if (closed.get()) {
                    throw new SessionClosedException();
                }
            } catch (final InterruptedException ignore) {
                throw requestInterrupted(requestNumber);
            }
        }
    }

    private void serverInvoke(final int requestNumber, final Request request) throws Exception {
        final Server.Invocation invocation = setup.server.invocation(request);
        setup.dispatcher.invoke(session, invocation, () -> {
            try {
                final Reply reply = invocation.invoke(interceptor);
                if (!invocation.oneWay) {
                    write(requestNumber, reply);
                }
            } catch (final Exception e) {
                close(e);
            }
        });
    }

    /**
     * Must be called if a packet has been received.
     * It must also be called if {@link Packet#isEnd()}; however, it must not be called again after that.
     */
    public void received(final Packet packet) {
        try {
            if (packet.isEnd()) {
                close(false, null);
                return;
            }
            final Message message = packet.message();
            if (message instanceof Request) {
                serverInvoke(packet.requestNumber(), (Request)message);
            } else {
                replyReceived(packet.requestNumber(), (Reply)message);
            }
        } catch (final Exception e) {
            close(e);
        }
    }

    /**
     * Must be called if communication has failed.
     * This method is idempotent.
     */
    public void close(final Throwable throwable) {
        close(false, Check.notNull(throwable));
    }

    private final AtomicInteger nextRequestNumber = new AtomicInteger(Packet.END_REQUEST_NUMBER);

    @Override protected Object invoke(final Client.Invocation invocation) throws Throwable {
        return invocation.invoke(interceptor, request -> {
            int requestNumber;
            do { // we can't use END_REQUEST_NUMBER as regular requestNumber
                requestNumber = nextRequestNumber.incrementAndGet();
            } while (requestNumber == Packet.END_REQUEST_NUMBER);
            if (invocation.oneWay) {
                write(requestNumber, request);
                return null;
            }
            return clientRpcInvoke(requestNumber, request);
        });
    }

    /**
     * This method is idempotent.
     */
    void close() {
        close(true, null);
    }

}
