package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.serialize.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Writes to socket in a writer thread.
 * Caller thread never blocks.
 * Closes {@link Session} if writer queue is full.
 */
public final class AsyncSocketConnection extends SocketConnection {

    private final Executor writerExecutor;
    private volatile boolean closed = false;
    private final BlockingQueue<ByteArrayOutputStream> writerQueue;

    private AsyncSocketConnection(final Serializer packetSerializer, final Socket socket, final OutputStream out, final Executor writerExecutor, final int writerQueueSize) {
        super(packetSerializer, socket, out);
        this.writerExecutor = writerExecutor;
        writerQueue = new ArrayBlockingQueue<>(writerQueueSize);
    }

    @Override protected void created(final Session session) {
        writerExecutor.execute(() -> {
            try {
                while (true) {
                    final var buffer = writerQueue.poll(1L, TimeUnit.SECONDS);
                    if (buffer == null) {
                        if (closed) {
                            return;
                        }
                        continue;
                    }
                    while (true) { // drain queue -> batching of packets
                        final var buffer2 = writerQueue.poll();
                        if (buffer2 == null) {
                            break;
                        }
                        buffer2.writeTo(buffer);
                    }
                    flush(buffer);
                }
            } catch (final Exception e) {
                Session.close(session, e);
            }
        });
    }

    @Override public void write(final Packet packet) throws Exception {
        if (!writerQueue.offer(writeToBuffer(packet))) {
            throw new RuntimeException("writer queue full");
        }
    }

    @Override public void closed() throws Exception {
        try {
            TimeUnit.SECONDS.sleep(1L); // give the socket a chance to write the end packet
        } finally {
            closed = true; // terminates writer thread
            super.closed();
        }
    }

    /**
     * @param writerExecutor used once for each session
     */
    public static Factory factory(final Executor writerExecutor, final int writerQueueSize) {
        Objects.requireNonNull(writerExecutor);
        if (writerQueueSize < 1) {
            throw new IllegalArgumentException("writerQueueSize < 1");
        }
        return (packetSerializer, socket, out) -> new AsyncSocketConnection(packetSerializer, socket, out, writerExecutor, writerQueueSize);
    }

}
