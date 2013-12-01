package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.util.Check;

import javax.net.SocketFactory;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

/**
 * Uses the same socket connection during a session.
 * <p/>
 * The semantic of the different executors is as follows:
 * <p/>
 * <table border = "1">
 *   <tr> <td>                  </td> <th> purpose             </th> <th> calls to execute </th> <th> terminates on      </th> </tr>
 *   <tr> <td> requestExecutor  </td> <td> executes request    </td> <td> for each request </td> <td> request executed   </td> </tr>
 *   <tr> <td> readerExecutor   </td> <td> reads from socket   </td> <td> once per session </td> <td> session.close()    </td> </tr>
 *   <tr> <td> writerExecutor   </td> <td> writes to socket    </td> <td> once per session </td> <td> session.close()    </td> </tr>
 *   <tr> <td> listenerExecutor </td> <td> accepts connections </td> <td> only once        </td> <td> thread.interrupt() </td> </tr>
 * </table>
 * <p/>
 * <pre>
 * SessionTransport transport = new SessionTransport(
 *   new SessionSetup(server, requestExecutor, sessionFactory),
 *   serializer,
 *   readerExecutor, writerExecutor,
 *   createSessionExceptionHandler
 * );
 *
 * start:
 * - server side: transport.start(listenerExecutor, address);
 * - client side: transport.connect(address);
 *
 * server side shutdown sequence:
 * - shutdown listenerExecutor
 * - close all open sessions
 * - shutdown readerExecutor & writerExecutor
 * - shutdown requestExecutor
 * <pre>
 */
public final class SessionTransport extends SocketListener {

  final SessionSetup setup;
  final Serializer packetSerializer;
  private final Executor readerExecutor;
  final Executor writerExecutor;
  final UncaughtExceptionHandler createSessionExceptionHandler;

  /**
   * @param createSessionExceptionHandler handles exceptions from {@link SessionFactory#create(SessionSetup, Connection)}
   */
  public SessionTransport(
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

  @Override void accept(final Socket adoptSocket) {
    execute(readerExecutor, createSessionExceptionHandler, adoptSocket, new Runnable() {
      @Override public void run() {
        SocketConnection.create(SessionTransport.this, adoptSocket);
      }
    });
  }

  @SuppressWarnings("WeakerAccess")
  public void connect(final SocketFactory socketFactory, final SocketAddress socketAddress) {
    final Socket socket;
    try {
      socket = connectSocket(socketFactory, socketAddress);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    accept(socket);
  }

  /**
   * Uses {@link SocketFactory#getDefault()}.
   */
  public void connect(final SocketAddress socketAddress) {
    connect(SocketFactory.getDefault(), socketAddress);
  }

}
