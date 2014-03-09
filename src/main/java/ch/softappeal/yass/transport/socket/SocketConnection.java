package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.TransportSetup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class SocketConnection extends Connection {

  private final Serializer packetSerializer;
  public final Socket socket;
  private volatile boolean closed = false;
  private final BlockingQueue<ByteArrayOutputStream> writerQueue = new LinkedBlockingQueue<ByteArrayOutputStream>(); // unbounded queue
  private final Object writerQueueEmpty = new Object();

  SocketConnection(
    final TransportSetup setup, final Socket socket, final Reader reader, final OutputStream outputStream, final Executor writerExecutor
  ) throws Exception {
    packetSerializer = setup.packetSerializer;
    this.socket = socket;
    final Session session = setup.createSession(this);
    if (!open(session)) {
      return;
    }
    try {
      writerExecutor.execute(new Runnable() {
        @Override public void run() {
          try {
            write(outputStream);
          } catch (final Exception e) {
            close(session, e);
          }
        }
      });
    } catch (final Exception e) {
      close(session, e);
      return;
    }
    read(session, reader);
  }

  @Override protected void write(final Packet packet) throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
    packetSerializer.write(packet, Writer.create(buffer));
    writerQueue.put(buffer);
  }

  private void read(final Session session, final Reader reader) {
    while (true) {
      final Packet packet;
      try {
        packet = (Packet)packetSerializer.read(reader);
      } catch (final Exception e) {
        close(session, e);
        return;
      }
      received(session, packet);
      if (packet.isEnd()) {
        return;
      }
    }
  }

  private void notifyWriterQueueEmpty() {
    synchronized (writerQueueEmpty) {
      writerQueueEmpty.notifyAll();
    }
  }

  /**
   * Buffering of output is needed to prevent long delays due to Nagle's algorithm.
   */
  private static void flush(final ByteArrayOutputStream buffer, final OutputStream out) throws IOException {
    buffer.writeTo(out);
    out.flush();
  }

  private void write(final OutputStream out) throws Exception {
    while (true) {
      final ByteArrayOutputStream buffer = writerQueue.poll(100L, TimeUnit.MILLISECONDS);
      if (buffer == null) {
        if (closed) {
          return;
        }
        continue;
      }
      while (true) { // drain queue -> batching of packets
        final ByteArrayOutputStream buffer2 = writerQueue.poll();
        if (buffer2 == null) {
          notifyWriterQueueEmpty();
          break;
        }
        buffer2.writeTo(buffer);
      }
      flush(buffer, out);
    }
  }

  private boolean writerQueueFull() {
    return !writerQueue.isEmpty();
  }

  public void awaitWriterQueueEmpty() {
    try {
      synchronized (writerQueueEmpty) {
        while (writerQueueFull()) {
          writerQueueEmpty.wait();
        }
      }
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Note: No more calls to {@link #write(Packet)} are accepted when this method is called due to implementation of {@link Session}.
   */
  @Override protected void closed() throws Exception {
    try {
      try {
        while (writerQueueFull()) {
          TimeUnit.MILLISECONDS.sleep(100L);
        }
        TimeUnit.MILLISECONDS.sleep(100L); // give the socket a chance to write the end packet
      } finally {
        closed = true; // terminates writer thread
        socket.close();
      }
    } finally {
      notifyWriterQueueEmpty(); // guarantees that awaitWriterQueueEmpty never blocks again
    }
  }

}
