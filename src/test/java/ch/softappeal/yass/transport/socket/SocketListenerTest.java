package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.TestUtils;
import ch.softappeal.yass.util.test.RejectExecutor;
import org.junit.Assert;
import org.junit.Test;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketListenerTest {

  public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

  private static final SocketListener LISTENER = new SocketListener() {
    @Override void accept(final Socket adoptSocket) {
      try {
        adoptSocket.close();
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
  };

  public static void shutdown(final ExecutorService executorService) throws InterruptedException {
    executorService.shutdownNow();
    TimeUnit.MILLISECONDS.sleep(3 * SocketListener.ACCEPT_TIMEOUT_MILLISECONDS);
  }

  @Test public void test() throws Exception {
    try {
      SocketListener.connectSocket(SocketFactory.getDefault(), ADDRESS);
      Assert.fail();
    } catch (final IOException e) {
      System.out.println(e);
    }
    ExecutorService executor;
    executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    LISTENER.start(executor, ADDRESS);
    final Socket socket = SocketListener.connectSocket(SocketFactory.getDefault(), ADDRESS);
    socket.close();
    try {
      LISTENER.start(executor, ADDRESS);
      Assert.fail();
    } catch (final RuntimeException e) {
      System.out.println(e);
    }
    shutdown(executor);
    executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    LISTENER.start(executor, ADDRESS);
    shutdown(executor);
    try {
      LISTENER.start(RejectExecutor.INSTANCE, ADDRESS);
      Assert.fail();
    } catch (final RuntimeException e) {
      System.out.println(e);
    }
  }

}
