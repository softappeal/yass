package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.session.test.LocalConnectionTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.StringPathSerializer;
import ch.softappeal.yass.transport.socket.PathResolver;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketTransportTest extends InvokeTest {

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

  public static final String PATH = "test";

  @Test public void createException() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(StringPathSerializer.INSTANCE, new PathResolver(PATH, createTransport(false, "server", executor, false, false, false)), executor, TestUtils.TERMINATE).start(executor, SocketListenerTest.ADDRESS);
      createTransport(false, "client", executor, true, false, false).connect(SocketListenerTest.ADDRESS, StringPathSerializer.INSTANCE, PATH);
      TimeUnit.MILLISECONDS.sleep(200L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void openedException() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(StringPathSerializer.INSTANCE, new PathResolver(PATH, createTransport(false, "server", executor, false, true, false)), executor, TestUtils.TERMINATE).start(executor, SocketListenerTest.ADDRESS);
      createTransport(false, "client", executor, false, true, false).connect(SocketListenerTest.ADDRESS, StringPathSerializer.INSTANCE, PATH);
      TimeUnit.MILLISECONDS.sleep(200L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void invokeBeforeOpened() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(StringPathSerializer.INSTANCE, new PathResolver(PATH, createTransport(false, "server", executor, false, false, false)), executor, TestUtils.TERMINATE).start(executor, SocketListenerTest.ADDRESS);
      createTransport(false, "client", executor, false, false, true).connect(SocketListenerTest.ADDRESS, StringPathSerializer.INSTANCE, PATH);
      TimeUnit.MILLISECONDS.sleep(200L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void clientInvoke() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(StringPathSerializer.INSTANCE, new PathResolver(PATH, createTransport(false, "server", executor, false, false, false)), executor, TestUtils.TERMINATE).start(executor, SocketListenerTest.ADDRESS);
      createTransport(true, "client", executor, false, false, false).connect(SocketListenerTest.ADDRESS, StringPathSerializer.INSTANCE, PATH);
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void serverInvoke() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(StringPathSerializer.INSTANCE, new PathResolver(PATH, createTransport(true, "server", executor, false, false, false)), executor, TestUtils.TERMINATE).start(executor, SocketListenerTest.ADDRESS);
      createTransport(false, "client", executor, false, false, false).connect(SocketListenerTest.ADDRESS, StringPathSerializer.INSTANCE, PATH);
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void wrongPath() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(StringPathSerializer.INSTANCE, new PathResolver(PATH, createTransport(true, "server", executor, false, false, false)), executor, Exceptions.STD_ERR).start(executor, SocketListenerTest.ADDRESS);
      createTransport(false, "client", executor, false, false, false).connect(SocketListenerTest.ADDRESS, StringPathSerializer.INSTANCE, "wrongPath");
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void multiplePathes() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    final String path1 = "path1";
    final String path2 = "path2";
    final Map<String, SocketTransport> pathMappings = new HashMap<>(2);
    pathMappings.put(path1, createTransport(true, "server1", executor, false, false, false));
    pathMappings.put(path2, createTransport(true, "server2", executor, false, false, false));
    try {
      SocketTransport.listener(
        StringPathSerializer.INSTANCE,
        new PathResolver(pathMappings),
        executor,
        TestUtils.TERMINATE
      ).start(executor, SocketListenerTest.ADDRESS);
      createTransport(false, "client1", executor, false, false, false).connect(SocketListenerTest.ADDRESS, StringPathSerializer.INSTANCE, path1);
      TimeUnit.MILLISECONDS.sleep(400L);
      createTransport(false, "client2", executor, false, false, false).connect(SocketListenerTest.ADDRESS, StringPathSerializer.INSTANCE, path2);
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

}
