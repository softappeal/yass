package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.session.test.LocalConnectionTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SessionTransportTest extends InvokeTest {

  private static SocketTransport createTransport(
    final boolean invoke, final String name, final Executor executor,
    final boolean createException, final boolean openedException, final boolean invokeBeforeOpened
  ) {
    return new SocketTransport(
      LocalConnectionTest.createSetup(invoke, name, executor, createException, openedException, invokeBeforeOpened),
      PacketSerializerTest.SERIALIZER,
      executor, executor,
      Exceptions.STD_ERR
    );
  }

  @Test public void createException() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      createTransport(false, "server", executor, false, false, false).start(executor, SocketListenerTest.ADDRESS);
      createTransport(false, "client", executor, true, false, false).connect(SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(200L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void openedException() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      createTransport(false, "server", executor, false, true, false).start(executor, SocketListenerTest.ADDRESS);
      createTransport(false, "client", executor, false, true, false).connect(SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(200L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void invokeBeforeOpened() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      createTransport(false, "server", executor, false, false, false).start(executor, SocketListenerTest.ADDRESS);
      createTransport(false, "client", executor, false, false, true).connect(SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(200L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void clientInvoke() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      createTransport(false, "server", executor, false, false, false).start(executor, SocketListenerTest.ADDRESS);
      createTransport(true, "client", executor, false, false, false).connect(SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void serverInvoke() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      createTransport(true, "server", executor, false, false, false).start(executor, SocketListenerTest.ADDRESS);
      createTransport(false, "client", executor, false, false, false).connect(SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

}
