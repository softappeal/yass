package ch.softappeal.yass.transport.socket;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public abstract class SocketListener {

  /**
   * Should not block for long.
   * @param adoptSocket must be closed by this method
   */
  abstract void accept(Socket adoptSocket);

  static void execute(final Executor executor, final UncaughtExceptionHandler exceptionHandler, final Socket adoptSocket, final Runnable runnable) {
    try {
      executor.execute(runnable);
    } catch (final Exception e) {
      closeWithAddSuppressed(adoptSocket, e);
      exceptionHandler.uncaughtException(Thread.currentThread(), e);
    }
  }

  static final int ACCEPT_TIMEOUT_MILLISECONDS = 100;

  /**
   * Starts a socket listener on the server side.
   * @param listenerExecutor must interrupt it's threads to terminate the socket listener (-> use {@link ExecutorService#shutdownNow()})
   */
  @SuppressWarnings("WeakerAccess")
  public final void start(final Executor listenerExecutor, final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
    try {
      final ServerSocket serverSocket = socketFactory.createServerSocket();
      try {
        serverSocket.bind(socketAddress);
        serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MILLISECONDS);
        listenerExecutor.execute(new Runnable() {

          @SuppressWarnings("BreakStatement")
          private void loop() throws IOException {
            //noinspection LoopWithImplicitTerminationCondition
            while (true) {
              if (Thread.interrupted()) {
                break;
              }
              final Socket socket;
              //noinspection UnusedCatchParameter
              try {
                //noinspection SocketOpenedButNotSafelyClosed
                socket = serverSocket.accept();
              } catch (final SocketTimeoutException ignore) { // thrown if SoTimeout reached
                //noinspection ContinueStatement
                continue;
              } catch (final InterruptedIOException ignore) {
                break; // needed because some VM's (for example: Sun Solaris) throw this exception if the thread gets interrupted
              }
              accept(socket);
            }
          }

          @Override public void run() {
            try {
              try {
                loop();
              } catch (final Exception e) {
                closeWithAddSuppressed(serverSocket, e);
                throw e;
              }
              serverSocket.close();
            } catch (final IOException e) {
              throw new RuntimeException(e);
            }
          }

        });
      } catch (final Exception e) {
        closeWithAddSuppressed(serverSocket, e);
        throw e;
      }
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Uses {@link ServerSocketFactory#getDefault()}.
   * @see #start(Executor, ServerSocketFactory, SocketAddress)
   */
  public final void start(final Executor listenerExecutor, final SocketAddress socketAddress) {
    start(listenerExecutor, ServerSocketFactory.getDefault(), socketAddress);
  }

  static void closeWithAddSuppressed(final ServerSocket serverSocket, final Exception e) {
    try {
      serverSocket.close();
    } catch (final Exception e2) {
      e.addSuppressed(e2);
    }
  }

  static void closeWithAddSuppressed(final Socket socket, final Exception e) {
    try {
      socket.close();
    } catch (final Exception e2) {
      e.addSuppressed(e2);
    }
  }

  /**
   * Forces immediate send.
   */
  static void setTcpNoDelay(final Socket socket) throws IOException {
    socket.setTcpNoDelay(true);
  }

  /**
   * Buffering of output is needed to prevent long delays due to Nagle's algorithm.
   */
  static void flush(final ByteArrayOutputStream buffer, final OutputStream out) throws IOException {
    buffer.writeTo(out);
    out.flush();
  }

  static Socket connectSocket(final SocketFactory socketFactory, final SocketAddress socketAddress) throws IOException {
    final Socket socket = socketFactory.createSocket();
    try {
      socket.connect(socketAddress);
      return socket;
    } catch (final Exception e) {
      closeWithAddSuppressed(socket, e);
      throw e;
    }
  }

}
