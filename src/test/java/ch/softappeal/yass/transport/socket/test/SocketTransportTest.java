package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.session.test.LocalConnectionTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.StringPathSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketExecutor;
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

  private static TransportSetup createSetup(
    final boolean invoke, final String name, final Executor executor,
    final boolean createException, final boolean openedException, final boolean invokeBeforeOpened
  ) {
    return new TransportSetup(
      LocalConnectionTest.createSetup(invoke, name, executor, createException, openedException, invokeBeforeOpened),
      PacketSerializerTest.SERIALIZER
    );
  }

  public static final String PATH = "test";
  public static final Serializer PATH_SERIALIZER = StringPathSerializer.INSTANCE;

  @Test public void createException() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    try {
      SocketTransport.listener(PATH_SERIALIZER, new PathResolver(PATH, createSetup(false, "server", executor, false, false, false))).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(createSetup(false, "client", executor, true, false, false), new SocketExecutor(executor, TestUtils.TERMINATE), PATH_SERIALIZER, PATH, SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(200L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void openedException() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(PATH_SERIALIZER, new PathResolver(PATH, createSetup(false, "server", executor, false, true, false))).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(createSetup(false, "client", executor, false, true, false), new SocketExecutor(executor, TestUtils.TERMINATE), PATH_SERIALIZER, PATH, SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(200L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void invokeBeforeOpened() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    try {
      SocketTransport.listener(PATH_SERIALIZER, new PathResolver(PATH, createSetup(false, "server", executor, false, false, false))).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(createSetup(false, "client", executor, false, false, true), new SocketExecutor(executor, TestUtils.TERMINATE), PATH_SERIALIZER, PATH, SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(200L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void clientInvoke() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(PATH_SERIALIZER, new PathResolver(PATH, createSetup(false, "server", executor, false, false, false))).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(createSetup(true, "client", executor, false, false, false), new SocketExecutor(executor, TestUtils.TERMINATE), PATH_SERIALIZER, PATH, SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void serverInvoke() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(PATH_SERIALIZER, new PathResolver(PATH, createSetup(true, "server", executor, false, false, false))).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(createSetup(false, "client", executor, false, false, false), new SocketExecutor(executor, TestUtils.TERMINATE), PATH_SERIALIZER, PATH, SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void wrongPath() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    try {
      SocketTransport.listener(PATH_SERIALIZER, new PathResolver(PATH, createSetup(true, "server", executor, false, false, false))).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(createSetup(false, "client", executor, false, false, false), new SocketExecutor(executor, TestUtils.TERMINATE), PATH_SERIALIZER, "wrongPath", SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void multiplePathes() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    final String path1 = "path1";
    final String path2 = "path2";
    final Map<String, TransportSetup> pathMappings = new HashMap<String, TransportSetup>(2);
    pathMappings.put(path1, createSetup(true, "server1", executor, false, false, false));
    pathMappings.put(path2, createSetup(true, "server2", executor, false, false, false));
    try {
      SocketTransport.listener(
        PATH_SERIALIZER, new PathResolver(pathMappings)
      ).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(createSetup(false, "client1", executor, false, false, false), new SocketExecutor(executor, TestUtils.TERMINATE), PATH_SERIALIZER, path1, SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(400L);
      SocketTransport.connect(createSetup(false, "client2", executor, false, false, false), new SocketExecutor(executor, TestUtils.TERMINATE), PATH_SERIALIZER, path2, SocketListenerTest.ADDRESS);
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

}
