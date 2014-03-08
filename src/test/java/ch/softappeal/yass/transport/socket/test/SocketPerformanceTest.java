package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.test.SerializerTest;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.PacketSerializer;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.StringPathSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketExecutor;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketPerformanceTest extends InvokeTest {

  public static final int COUNTER = 1;

  public static final Serializer MESSAGE_SERIALIZER = new MessageSerializer(SerializerTest.TAGGED_FAST_SERIALIZER);

  public static final Serializer PACKET_SERIALIZER = new PacketSerializer(MESSAGE_SERIALIZER);

  private static TransportSetup createSetup(final Executor executor, @Nullable final CountDownLatch latch) {
    return new TransportSetup(PerformanceTest.createSetup(executor, latch, COUNTER), PACKET_SERIALIZER);
  }

  @Test public void test() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      SocketTransport.listener(StringPathSerializer.INSTANCE, new PathResolver(SocketTransportTest.PATH, createSetup(executor, null))).start(executor, new SocketExecutor(executor, TestUtils.TERMINATE), SocketListenerTest.ADDRESS);
      final CountDownLatch latch = new CountDownLatch(1);
      SocketTransport.connect(createSetup(executor, latch), new SocketExecutor(executor, TestUtils.TERMINATE), StringPathSerializer.INSTANCE, SocketTransportTest.PATH, SocketListenerTest.ADDRESS);
      latch.await();
      TimeUnit.MILLISECONDS.sleep(100L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

}
