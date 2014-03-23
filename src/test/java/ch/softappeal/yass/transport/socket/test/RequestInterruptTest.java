package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.RequestInterruptedException;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.StringPathSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketExecutor;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RequestInterruptTest extends InvokeTest {

  @Test public void test() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    try {
      SocketTransport.listener(
        StringPathSerializer.INSTANCE,
        new PathResolver(
          SocketTransportTest.PATH,
          new TransportSetup(
            new Server(TaggedMethodMapper.FACTORY, new Service(ContractIdTest.ID, new TestServiceImpl())),
            executor,
            PacketSerializerTest.SERIALIZER
          ) {
            @Override public Session createSession(final SessionClient sessionClient) {
              return new Session(sessionClient) {
                @Override public void closed(@Nullable final Throwable throwable) {
                  Exceptions.STD_ERR.uncaughtException(Thread.currentThread(), throwable);
                }
              };
            }
          }
        )
      ).start(executor, new SocketExecutor(executor, Exceptions.TERMINATE), SocketListenerTest.ADDRESS);
      SocketTransport.connect(
        new TransportSetup(
          new Server(TaggedMethodMapper.FACTORY),
          executor,
          PacketSerializerTest.SERIALIZER
        ) {
          @Override public Session createSession(final SessionClient sessionClient) {
            return new Session(sessionClient) {
              @Override public void opened() {
                final TestService testService = invoker(ContractIdTest.ID).proxy(new Interceptor() {
                  @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
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
              @Override public void closed(@Nullable final Throwable throwable) {
                Exceptions.STD_ERR.uncaughtException(Thread.currentThread(), throwable);
              }
            };
          }
        },
        new SocketExecutor(executor, Exceptions.TERMINATE), StringPathSerializer.INSTANCE, SocketTransportTest.PATH, SocketListenerTest.ADDRESS
      );
      TimeUnit.SECONDS.sleep(1L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

}
