package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.fast.TaggedFastSerializer;
import ch.softappeal.yass.serialize.fast.TypeConverterId;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.PacketSerializer;
import ch.softappeal.yass.transport.socket.SessionTransport;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.StatelessTransport;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.PerformanceTask;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketPerformanceTest extends InvokeTest {

  private static final int COUNTER = 1;

  private static final Serializer FAST_SERIALIZER = new TaggedFastSerializer(
    FastReflector.FACTORY,
    Arrays.<TypeConverterId>asList(),
    Arrays.<Class<?>>asList(),
    Arrays.<Class<?>>asList(),
    Arrays.<Class<?>>asList()
  );

  public static final Serializer MESSAGE_SERIALIZER = new MessageSerializer(FAST_SERIALIZER);

  public static final Serializer PACKET_SERIALIZER = new PacketSerializer(MESSAGE_SERIALIZER);

  private static SessionTransport createTransport(final Executor executor, @Nullable final CountDownLatch latch) {
    return new SessionTransport(
      PerformanceTest.createSetup(executor, latch, COUNTER),
      PACKET_SERIALIZER,
      executor, executor,
      TestUtils.TERMINATE
    );
  }

  @Test public void session() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      createTransport(executor, null).start(executor, SocketListenerTest.ADDRESS);
      final CountDownLatch latch = new CountDownLatch(1);
      createTransport(executor, latch).connect(SocketListenerTest.ADDRESS);
      latch.await();
      TimeUnit.MILLISECONDS.sleep(100L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void stateless() throws Exception {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      new StatelessTransport(
        new Server(PerformanceTest.METHOD_MAPPER_FACTORY, PerformanceTest.CONTRACT_ID.service(new TestServiceImpl())),
        MESSAGE_SERIALIZER,
        executor,
        Exceptions.STD_ERR
      ).start(executor, SocketListenerTest.ADDRESS);
      final TestService testService = PerformanceTest.CONTRACT_ID.invoker(StatelessTransport.client(
        PerformanceTest.METHOD_MAPPER_FACTORY, MESSAGE_SERIALIZER, SocketListenerTest.ADDRESS
      )).proxy();
      System.out.println("*** rpc");
      new PerformanceTask() {
        @Override protected void run(final int count) throws DivisionByZeroException {
          int counter = count;
          while (counter-- > 0) {
            Assert.assertTrue(testService.divide(12, 4) == 3);
          }
        }
      }.run(COUNTER, TimeUnit.MICROSECONDS);
      System.out.println("*** oneway");
      new PerformanceTask() {
        @Override protected void run(final int count) {
          int counter = count;
          while (counter-- > 0) {
            testService.oneWay(-1);
          }
        }
      }.run(COUNTER, TimeUnit.MICROSECONDS);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

}
