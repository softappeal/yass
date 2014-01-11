package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Packet;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class SocketConnection extends Connection {

  private final Reader reader;
  private final Serializer packetSerializer;
  public final Socket socket;
  private volatile boolean closed = false;
  private final BlockingQueue<ByteArrayOutputStream> writerQueue = new LinkedBlockingQueue<>(); // unbounded queue
  private final Object writerQueueEmpty = new Object();

  private SocketConnection(final Serializer packetSerializer, final Socket adoptSocket) throws IOException {
    this.packetSerializer = packetSerializer;
    socket = adoptSocket;
    SocketListener.setTcpNoDelay(socket);
    reader = Reader.create(socket.getInputStream());
  }

  @Override protected void write(final Packet packet) throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
    packetSerializer.write(packet, Writer.create(buffer));
    writerQueue.put(buffer);
  }

  private void read(final Session session) {
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
      SocketListener.flush(buffer, out);
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

  private void created(final Session session, final Executor writerExecutor) {
    if (!open(session)) {
      return;
    }
    try {
      writerExecutor.execute(new Runnable() {
        @Override public void run() {
          try {
            write(socket.getOutputStream());
          } catch (final Exception e) {
            close(session, e);
          }
        }
      });
    } catch (final Exception e) {
      close(session, e);
      return;
    }
    read(session);
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

  static void create(final SocketTransport transport, final Socket adoptSocket) {
    final SocketConnection connection;
    final Session session;
    try {
      connection = new SocketConnection(transport.packetSerializer, adoptSocket);
      session = transport.setup.createSession(connection);
    } catch (final Exception e) {
      SocketListener.close(adoptSocket, e);
      transport.createSessionExceptionHandler.uncaughtException(Thread.currentThread(), e);
      return;
    }
    connection.created(session, transport.writerExecutor);
  }

}
