package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.ExceptionReply;
import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server.ServerInvocation;
import ch.softappeal.yass.core.remote.Tunnel;
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
public abstract class Session extends Client implements AutoCloseable {

  private static final ThreadLocal<Session> INSTANCE = new ThreadLocal<>();

  /**
   * @see Interceptors#hasInvocation(ThreadLocal)
   */
  public static boolean hasInvocation() {
    return Interceptors.hasInvocation(INSTANCE);
  }

  /**
   * @return the session of the active invocation
   * @see Interceptors#getInvocation(ThreadLocal)
   */
  public static Session get() {
    return Interceptors.getInvocation(INSTANCE);
  }

  private final SessionSetup setup;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  public final Connection connection;
  private final Interceptor sessionInterceptor = Interceptors.threadLocal(INSTANCE, this);
  private boolean opened = false;

  protected Session(final SessionSetup setup, final Connection connection) {
    super(setup.server.methodMapperFactory);
    this.setup = setup;
    this.connection = Check.notNull(connection);
  }

  /**
   * Called from {@link SessionSetup#requestExecutor} when {@link Session} is fully usable.
   * <p/>
   * This implementation does nothing.
   * @throws Exception if an exception is thrown, {@link #closed(Exception)} will be called
   */
  protected void opened() throws Exception {
    // empty
  }

  /**
   * Called when the {@link Session} has been closed.
   * @param exception null if regular close else reason for close
   */
  protected abstract void closed(@Nullable Exception exception);

  /**
   * Must be called once immediately after calling {@link SessionSetup#createSession(Connection)}.
   * @return false if {@code requestExecutor} rejected the task
   */
  final boolean open() {
    opened = true;
    try {
      setup.requestExecutor.execute(new Runnable() {
        @Override public void run() {
          try {
            opened();
          } catch (final Exception e) {
            close(e);
          }
        }
      });
    } catch (final Exception e) {
      close(e);
      return false;
    }
    return true;
  }

  private void close(final boolean sendEnd, @Nullable final Exception exception) {
    try {
      if (closed.getAndSet(true)) {
        return;
      }
      try {
        closed(exception);
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

  private void write(final int requestNumber, final Message message) {
    if (closed.get()) {
      throw new SessionClosedException();
    }
    try {
      connection.write(new Packet(requestNumber, message));
    } catch (final Exception e) {
      close(e);
    }
  }

  private final Map<Integer, BlockingQueue<Reply>> requestNumber2replyQueue = Collections.synchronizedMap(new HashMap<Integer, BlockingQueue<Reply>>(16));

  private void writeReply(final int requestNumber, final Reply reply) throws InterruptedException {
    @Nullable final BlockingQueue<Reply> replyQueue = requestNumber2replyQueue.remove(requestNumber);
    if (replyQueue != null) { // needed because request can be interrupted, see below
      replyQueue.put(reply);
    }
  }

  private Reply requestInterrupted(final int requestNumber) {
    requestNumber2replyQueue.remove(requestNumber);
    return new ExceptionReply(new RequestInterruptedException());
  }

  private Reply writeRequestAndReadReply(final int requestNumber, final Request request) {
    final BlockingQueue<Reply> replyQueue = new ArrayBlockingQueue<>(1, false); // we use unfair for speed
    if (requestNumber2replyQueue.put(requestNumber, replyQueue) != null) {
      throw new RuntimeException("already waiting for requestNumber " + requestNumber);
    }
    write(requestNumber, request); // note: must be after line above to prevent race conditions with method above
    while (true) {
      if (Thread.interrupted()) {
        return requestInterrupted(requestNumber);
      }
      try {
        final Reply reply = replyQueue.poll(100L, TimeUnit.MILLISECONDS);
        if (reply != null) {
          return reply;
        } else if (closed.get()) {
          throw new SessionClosedException();
        }
      } catch (final InterruptedException ignored) {
        return requestInterrupted(requestNumber);
      }
    }
  }

  private void serverInvoke(final int requestNumber, final Request request) {
    setup.requestExecutor.execute(new Runnable() {
      @Override public void run() {
        try {
          final ServerInvocation invocation = setup.server.invocation(request);
          final Reply reply = invocation.invoke(sessionInterceptor);
          if (!invocation.oneWay) {
            write(requestNumber, reply);
          }
        } catch (final Exception e) {
          close(e);
        }
      }
    });
  }

  /**
   * Must be called if a packet has been received.
   * It must also be called if {@link Packet#isEnd()}; however, it must not be called again after that.
   */
  final void received(final Packet packet) {
    try {
      if (packet.isEnd()) {
        close(null);
        return;
      }
      final Message message = packet.message();
      if (message instanceof Request) {
        serverInvoke(packet.requestNumber(), (Request)message);
      } else { // Reply
        writeReply(packet.requestNumber(), (Reply)message);
      }
    } catch (final Exception e) {
      close(e);
    }
  }

  /**
   * Must be called if communication has failed.
   * This method is idempotent.
   */
  final void close(final Exception e) {
    close(false, e);
  }

  private final AtomicInteger nextRequestNumber = new AtomicInteger(Packet.END_REQUEST_NUMBER);

  @Override protected final Object invoke(final ClientInvocation invocation) throws Throwable {
    if (!opened) {
      throw new IllegalStateException("session is not yet opened");
    }
    return invocation.invoke(sessionInterceptor, new Tunnel() {
      @Override public Reply invoke(final Request request) {
        int requestNumber;
        do { // we can't use END_REQUEST_NUMBER as regular requestNumber
          requestNumber = nextRequestNumber.incrementAndGet();
        } while (requestNumber == Packet.END_REQUEST_NUMBER);
        if (invocation.oneWay) {
          write(requestNumber, request);
          return null;
        }
        return writeRequestAndReadReply(requestNumber, request);
      }
    });
  }

  /**
   * This method is idempotent.
   */
  @Override public final void close() {
    close(true, null);
  }

}
