package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Message;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Server.ServerInvocation;
import ch.softappeal.yass.core.remote.Tunnel;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import javax.net.SocketFactory;
import java.io.ByteArrayOutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

/**
 * Opens/closes a socket connection for each request.
 */
public final class StatelessTransport extends SocketListener {

  private static final ThreadLocal<Socket> SOCKET = new ThreadLocal<>();

  /**
   * @return the {@link Socket} of the active invocation or null if no active invocation
   */
  @Nullable public static Socket socket() {
    return SOCKET.get();
  }

  private static void write(final Message message, final Socket socket, final Serializer messageSerializer) throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
    messageSerializer.write(message, Writer.create(buffer));
    flush(buffer, socket.getOutputStream());
  }

  private static Message read(final Socket socket, final Serializer messageSerializer) throws Exception {
    return (Message)messageSerializer.read(Reader.create(socket.getInputStream()));
  }

  private final Server server;
  private final Serializer messageSerializer;
  private final Executor requestExecutor;
  private final UncaughtExceptionHandler acceptExceptionHandler;

  public StatelessTransport(
    final Server server, final Serializer messageSerializer,
    final Executor requestExecutor, final UncaughtExceptionHandler acceptExceptionHandler
  ) {
    this.server = Check.notNull(server);
    this.messageSerializer = Check.notNull(messageSerializer);
    this.requestExecutor = Check.notNull(requestExecutor);
    this.acceptExceptionHandler = Check.notNull(acceptExceptionHandler);
  }

  @Override void accept(final Socket adoptSocket) {
    execute(requestExecutor, acceptExceptionHandler, adoptSocket, new Runnable() {
      @Override public void run() {
        try (Socket socket = adoptSocket) {
          setTcpNoDelay(socket);
          final ServerInvocation invocation = server.invocation((Request)read(socket, messageSerializer));
          final Reply reply = invocation.invoke(Interceptors.threadLocal(SOCKET, socket));
          if (!invocation.oneWay) {
            write(reply, socket, messageSerializer);
          }
        } catch (final Exception e) {
          throw Exceptions.wrap(e);
        }
      }
    });
  }

  public static Client client(
    final MethodMapper.Factory methodMapperFactory, final Serializer messageSerializer,
    final SocketFactory socketFactory, final SocketAddress socketAddress
  ) {
    Check.notNull(messageSerializer);
    Check.notNull(socketFactory);
    Check.notNull(socketAddress);
    return new Client(methodMapperFactory) {
      @Override protected Object invoke(final ClientInvocation invocation) throws Throwable {
        try (Socket socket = connectSocket(socketFactory, socketAddress)) {
          return invocation.invoke(Interceptors.threadLocal(SOCKET, socket), new Tunnel() {
            @Override public Reply invoke(final Request request) throws Exception {
              setTcpNoDelay(socket);
              write(request, socket, messageSerializer);
              return invocation.oneWay ? null : (Reply)read(socket, messageSerializer);
            }
          });
        }
      }
    };
  }

  /**
   * Uses {@link SocketFactory#getDefault()}.
   */
  public static Client client(
    final MethodMapper.Factory methodMapperFactory, final Serializer messageSerializer,
    final SocketAddress socketAddress
  ) {
    return client(methodMapperFactory, messageSerializer, SocketFactory.getDefault(), socketAddress);
  }

}
