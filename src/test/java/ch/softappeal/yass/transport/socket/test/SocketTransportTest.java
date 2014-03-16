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
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketTransportTest extends InvokeTest {

  public static final String PATH = "test";
  public static final Serializer PATH_SERIALIZER = StringPathSerializer.INSTANCE;

  @Test public void createException() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    try {
      SocketTransport.listener(
        PATH_SERIALIZER, new PathResolver(PATH, LocalConnectionTest.createSetup(false, executor, false))
      ).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(
        LocalConnectionTest.createSetup(false, executor, true), new SocketExecutor(executor, TestUtils.TERMINATE),
        PATH_SERIALIZER, PATH, SocketListenerTest.ADDRESS
      );
      TimeUnit.MILLISECONDS.sleep(200L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void clientInvoke() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(
        PATH_SERIALIZER, new PathResolver(PATH, LocalConnectionTest.createSetup(false, executor, false))
      ).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(
        LocalConnectionTest.createSetup(true, executor, false), new SocketExecutor(executor, TestUtils.TERMINATE),
        PATH_SERIALIZER, PATH, SocketListenerTest.ADDRESS
      );
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void serverInvoke() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(
        PATH_SERIALIZER, new PathResolver(PATH, LocalConnectionTest.createSetup(true, executor, false))
      ).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(
        LocalConnectionTest.createSetup(false, executor, false), new SocketExecutor(executor, TestUtils.TERMINATE),
        PATH_SERIALIZER, PATH, SocketListenerTest.ADDRESS
      );
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void wrongPath() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    try {
      SocketTransport.listener(
        PATH_SERIALIZER, new PathResolver(PATH, LocalConnectionTest.createSetup(true, executor, false))
      ).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(
        LocalConnectionTest.createSetup(false, executor, false), new SocketExecutor(executor, TestUtils.TERMINATE),
        PATH_SERIALIZER, "wrongPath", SocketListenerTest.ADDRESS
      );
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void multiplePathes() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    final String path1 = "path1";
    final String path2 = "path2";
    final Map<String, TransportSetup> pathMappings = new HashMap<>(2);
    pathMappings.put(path1, LocalConnectionTest.createSetup(true, executor, false));
    pathMappings.put(path2, LocalConnectionTest.createSetup(true, executor, false));
    try {
      SocketTransport.listener(
        PATH_SERIALIZER, new PathResolver(pathMappings)
      ).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(
        LocalConnectionTest.createSetup(false, executor, false), new SocketExecutor(executor, TestUtils.TERMINATE),
        PATH_SERIALIZER, path1, SocketListenerTest.ADDRESS
      );
      TimeUnit.MILLISECONDS.sleep(400L);
      SocketTransport.connect(
        LocalConnectionTest.createSetup(false, executor, false), new SocketExecutor(executor, TestUtils.TERMINATE),
        PATH_SERIALIZER, path2, SocketListenerTest.ADDRESS
      );
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

}
