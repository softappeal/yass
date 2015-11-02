package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.core.remote.session.SessionClosedException;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Writes to socket in a writer thread.
 * Caller thread never blocks.
 * Throws {@link SessionClosedException} if writer queue is full.
 */
public final class AsyncSocketConnection extends SocketConnection {

    /**
     * @param writerExecutor used once for each session
     */
    public static Factory factory(final Executor writerExecutor, final int writerQueueSize) {
        Check.notNull(writerExecutor);
        if (writerQueueSize < 1) {
            throw new IllegalArgumentException("writerQueueSize < 1");
        }
        return (setup, socket, out) -> new AsyncSocketConnection(setup, socket, out, writerExecutor, writerQueueSize);
    }

    private final Executor writerExecutor;
    private volatile boolean closed = false;
    private final BlockingQueue<ByteArrayOutputStream> writerQueue;

    private AsyncSocketConnection(final TransportSetup setup, final Socket socket, final OutputStream out, final Executor writerExecutor, final int writerQueueSize) {
        super(setup, socket, out);
        this.writerExecutor = writerExecutor;
        writerQueue = new ArrayBlockingQueue<>(writerQueueSize);
    }

    @Override protected void created(final SessionClient sessionClient) throws Exception {
        writerExecutor.execute(() -> {
            try {
                while (true) {
                    final ByteArrayOutputStream buffer = writerQueue.poll(200L, TimeUnit.MILLISECONDS);
                    if (buffer == null) {
                        if (closed) {
                            return;
                        }
                        continue;
                    }
                    while (true) { // drain queue -> batching of packets
                        final ByteArrayOutputStream buffer2 = writerQueue.poll();
                        if (buffer2 == null) {
                            break;
                        }
                        buffer2.writeTo(buffer);
                    }
                    flush(buffer, out);
                }
            } catch (final Exception e) {
                sessionClient.close(e);
            }
        });
    }

    @Override public void write(final Packet packet) throws Exception {
        if (!writerQueue.offer(writeToBuffer(packet))) {
            throw new SessionClosedException();
        }
    }

    /**
     * Note: No more calls to {@link #write(Packet)} are accepted when this method is called due to implementation of {@link SessionClient}.
     */
    @Override public void closed() throws Exception {
        try {
            while (!writerQueue.isEmpty()) {
                TimeUnit.MILLISECONDS.sleep(200L);
            }
            TimeUnit.MILLISECONDS.sleep(200L); // give the socket a chance to write the end packet
        } finally {
            closed = true; // terminates writer thread
            super.closed();
        }
    }

}
