package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executor;

/**
 * Uses the same socket connection during a session.
 * <p>
 * The semantic of the different executors is as follows:
 * <table summary="executors">
 * <tr> <td>                  </td> <th> purpose             </th> <th> calls to execute          </th> <th> terminates on      </th> </tr>
 * <tr> <td> requestExecutor  </td> <td> executes request    </td> <td> for each incoming request </td> <td> request executed   </td> </tr>
 * <tr> <td> socketExecutor   </td> <td> socket read/write   </td> <td> twice for each session    </td> <td> session.close()    </td> </tr>
 * <tr> <td> listenerExecutor </td> <td> accepts connections </td> <td> only once                 </td> <td> thread.interrupt() </td> </tr>
 * </table>
 * <pre>
 * server shutdown sequence:
 * - shutdown listenerExecutor
 * - close all open sessions
 * - shutdown socketExecutor
 * - shutdown requestExecutor
 * </pre>
 */
public final class SocketTransport {

  private SocketTransport() {
    // disable
  }

  /**
   * Forces immediate send.
   */
  private static void setTcpNoDelay(final Socket socket) throws SocketException {
    socket.setTcpNoDelay(true);
  }

  public static SocketListener listener(final Serializer pathSerializer, final PathResolver pathResolver) {
    Check.notNull(pathSerializer);
    Check.notNull(pathResolver);
    return new SocketListener() {
      @Override void accept(final Socket socket, final Executor writerExecutor) throws Exception {
        setTcpNoDelay(socket);
        final Reader reader = Reader.create(socket.getInputStream());
        final TransportSetup setup = pathResolver.resolvePath(pathSerializer.read(reader));
        new SocketConnection(setup, socket, reader, socket.getOutputStream(), writerExecutor);
      }
    };
  }

  static void close(final Socket socket, final Exception e) {
    try {
      socket.close();
    } catch (final Exception e2) {
      e.addSuppressed(e2);
    }
  }

  static Socket connectSocket(final SocketFactory socketFactory, final SocketAddress socketAddress) throws IOException {
    final Socket socket = socketFactory.createSocket();
    try {
      socket.connect(socketAddress);
      return socket;
    } catch (final Exception e) {
      close(socket, e);
      throw e;
    }
  }

  public static void connect(
    final TransportSetup setup, final SocketExecutor socketExecutor, final Serializer pathSerializer, final Object path,
    final SocketFactory socketFactory, final SocketAddress socketAddress
  ) {
    Check.notNull(setup);
    Check.notNull(pathSerializer);
    Check.notNull(path);
    final Socket socket;
    try {
      socket = connectSocket(socketFactory, socketAddress);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    socketExecutor.execute(socket, new SocketListener() {
      @Override void accept(final Socket socket, final Executor writerExecutor) throws Exception {
        setTcpNoDelay(socket);
        final OutputStream outputStream = socket.getOutputStream();
        pathSerializer.write(path, Writer.create(outputStream));
        outputStream.flush();
        new SocketConnection(setup, socket, Reader.create(socket.getInputStream()), outputStream, writerExecutor);
      }
    });
  }

  /**
   * Uses {@link SocketFactory#getDefault()}.
   */
  public static void connect(
    final TransportSetup setup, final SocketExecutor socketExecutor, final Serializer pathSerializer, final Object path,
    final SocketAddress socketAddress
  ) {
    connect(setup, socketExecutor, pathSerializer, path, SocketFactory.getDefault(), socketAddress);
  }

}
