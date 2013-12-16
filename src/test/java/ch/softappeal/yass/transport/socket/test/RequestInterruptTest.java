package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.MethodMappers;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.RequestInterruptedException;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.socket.SessionTransport;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RequestInterruptTest extends InvokeTest {

  @Test
  public void test() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    try {
      new SessionTransport(
        new SessionSetup(new Server(MethodMappers.STRING_FACTORY, ContractIdTest.ID.service(new TestServiceImpl())), executor, new SessionFactory() {
          @Override public Session create(final SessionSetup setup, final Connection connection) {
            return new Session(setup, connection) {
              @Override public void closed(@Nullable final Exception exception) {
                Exceptions.STD_ERR.uncaughtException(Thread.currentThread(), exception);
              }
            };
          }
        }),
        PacketSerializerTest.SERIALIZER, executor, executor, Exceptions.STD_ERR
      ).start(executor, SocketListenerTest.ADDRESS);
      new SessionTransport(
        new SessionSetup(new Server(MethodMappers.STRING_FACTORY), executor, new SessionFactory() {
          @Override public Session create(final SessionSetup setup, final Connection connection) {
            return new Session(setup, connection) {
              @Override public void opened() {
                final TestService testService = ContractIdTest.ID.invoker(this).proxy(new Interceptor() {
                  @Override public Object invoke(final Invocation invocation) throws Throwable {
                    System.out.println("before");
                    try {
                      final Object reply = invocation.proceed();
                      System.out.println("after");
                      return reply;
                    } catch (final Throwable throwable) {
                      System.out.println("after exception");
                      throwable.printStackTrace(System.out);
                      throw throwable;
                    }
                  }
                });
                final Thread testThread = Thread.currentThread();
                new Thread() {
                  @Override public void run() {
                    try {
                      TimeUnit.MILLISECONDS.sleep(200);
                      testThread.interrupt();
                    } catch (InterruptedException e) {
                      throw new RuntimeException(e);
                    }
                    super.run();
                  }
                }.start();
                try {
                  testService.delay(400);
                  Assert.fail();
                } catch (final RequestInterruptedException e) {
                  e.printStackTrace(System.out);
                }
                testService.delay(10);
              }
              @Override public void closed(@Nullable final Exception exception) {
                Exceptions.STD_ERR.uncaughtException(Thread.currentThread(), exception);
              }
            };
          }
        }),
        PacketSerializerTest.SERIALIZER, executor, executor, Exceptions.STD_ERR
      ).connect(SocketListenerTest.ADDRESS);
      TimeUnit.SECONDS.sleep(1L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

}
