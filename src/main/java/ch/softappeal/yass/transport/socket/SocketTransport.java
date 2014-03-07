package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

/**
 * Uses the same socket connection during a session.
 * <p/>
 * The semantic of the different executors is as follows:
 * <table border = "1">
 * <tr> <td>                      </td> <th> purpose             </th> <th> calls to execute                </th> <th> terminates on      </th> </tr>
 * <tr> <td> requestExecutor      </td> <td> executes request    </td> <td> for each incoming request       </td> <td> request executed   </td> </tr>
 * <tr> <td> readerExecutor       </td> <td> reads from socket   </td> <td> once during session             </td> <td> session.close()    </td> </tr>
 * <tr> <td> writerExecutor       </td> <td> writes to socket    </td> <td> once during session             </td> <td> session.close()    </td> </tr>
 * <tr> <td> pathResolverExecutor </td> <td> resolves path       </td> <td> once before session established </td> <td> path resolved      </td> </tr>
 * <tr> <td> listenerExecutor     </td> <td> accepts connections </td> <td> only once                       </td> <td> thread.interrupt() </td> </tr>
 * </table>
 * <pre>
 * server shutdown sequence:
 * - shutdown listenerExecutor
 * - shutdown pathResolverExecutor
 * - close all open sessions
 * - shutdown readerExecutor & writerExecutor
 * - shutdown requestExecutor
 * </pre>
 */
public final class SocketTransport {

  final SessionSetup setup;
  final Serializer packetSerializer;
  private final Executor readerExecutor;
  final Executor writerExecutor;
  final UncaughtExceptionHandler createSessionExceptionHandler;

  /**
   * @param createSessionExceptionHandler handles exceptions from {@link SessionFactory#create(SessionSetup, Connection)}
   */
  public SocketTransport(
    final SessionSetup setup, final Serializer packetSerializer,
    final Executor readerExecutor, final Executor writerExecutor,
    final UncaughtExceptionHandler createSessionExceptionHandler
  ) {
    this.setup = Check.notNull(setup);
    this.packetSerializer = Check.notNull(packetSerializer);
    this.readerExecutor = Check.notNull(readerExecutor);
    this.writerExecutor = Check.notNull(writerExecutor);
    this.createSessionExceptionHandler = Check.notNull(createSessionExceptionHandler);
  }

  /**
   * @param pathResolverExceptionHandler handles exceptions from {@link PathResolver#resolvePath(Object)}
   */
  public static SocketListener listener(
    final Serializer pathSerializer, final PathResolver pathResolver,
    final Executor pathResolverExecutor, final UncaughtExceptionHandler pathResolverExceptionHandler
  ) {
    Check.notNull(pathSerializer);
    Check.notNull(pathResolver);
    Check.notNull(pathResolverExecutor);
    Check.notNull(pathResolverExceptionHandler);
    return new SocketListener() {
      @Override void accept(final Socket adoptSocket) {
        execute(pathResolverExecutor, pathResolverExceptionHandler, adoptSocket, new Runnable() {
          @Override public void run() {
            final SocketTransport transport;
            final Reader reader;
            final OutputStream outputStream;
            try {
              setTcpNoDelay(adoptSocket);
              reader = Reader.create(adoptSocket.getInputStream());
              outputStream = adoptSocket.getOutputStream();
              transport = pathResolver.resolvePath(pathSerializer.read(reader));
            } catch (final Exception e) {
              close(adoptSocket, e);
              pathResolverExceptionHandler.uncaughtException(Thread.currentThread(), e);
              return;
            }
            execute(transport.readerExecutor, transport.createSessionExceptionHandler, adoptSocket, new Runnable() {
              @Override public void run() {
                new SocketConnection(transport, adoptSocket, reader, outputStream);
              }
            });
          }
        });
      }
    };
  }

  public void connect(
    final SocketFactory socketFactory, final SocketAddress socketAddress,
    final Serializer pathSerializer, final Object path
  ) {
    Check.notNull(pathSerializer);
    Check.notNull(path);
    final Socket socket;
    try {
      socket = SocketListener.connectSocket(socketFactory, socketAddress);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    SocketListener.execute(readerExecutor, createSessionExceptionHandler, socket, new Runnable() {
      @Override public void run() {
        final Reader reader;
        final OutputStream outputStream;
        try {
          SocketListener.setTcpNoDelay(socket);
          reader = Reader.create(socket.getInputStream());
          outputStream = socket.getOutputStream();
          pathSerializer.write(path, Writer.create(outputStream));
          outputStream.flush();
        } catch (final Exception e) {
          SocketListener.close(socket, e);
          throw Exceptions.wrap(e);
        }
        new SocketConnection(SocketTransport.this, socket, reader, outputStream);
      }
    });
  }

  /**
   * Uses {@link SocketFactory#getDefault()}.
   */
  public void connect(
    final SocketAddress socketAddress,
    final Serializer pathSerializer, final Object path
  ) {
    connect(SocketFactory.getDefault(), socketAddress, pathSerializer, path);
  }

}
