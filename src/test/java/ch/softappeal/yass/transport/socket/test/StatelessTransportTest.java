package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.MethodMappers;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.core.remote.test.RemoteTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.StatelessTransport;
import ch.softappeal.yass.transport.test.MessageSerializerTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatelessTransportTest extends InvokeTest {

  private static final class SocketInterceptor implements Interceptor {
    private final String side;
    SocketInterceptor(final String side) {
      this.side = side;
    }
    @Override public Object invoke(final Invocation invocation) throws Throwable {
      println(side, "entry", "", StatelessTransport.socket().getLocalPort());
      try {
        return invocation.proceed();
      } finally {
        println(side, "exit", "", StatelessTransport.socket().getLocalPort());
      }
    }
  }

  @Test public void test() throws Exception {
    try {
      StatelessTransport.socket();
      Assert.fail();
    } catch (final RuntimeException e) {
      Assert.assertEquals("no active invocation", e.getMessage());
    }
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      new StatelessTransport(
        new Server(
          MethodMappers.TAG_FACTORY,
          ContractIdTest.ID.service(new TestServiceImpl(), new SocketInterceptor("Server"), RemoteTest.CONTRACT_ID_CHECKER, SERVER_INTERCEPTOR)
        ),
        MessageSerializerTest.SERIALIZER,
        executor,
        Exceptions.STD_ERR
      ).start(executor, SocketListenerTest.ADDRESS);
      invoke(
        ContractIdTest.ID.invoker(StatelessTransport.client(
          MethodMappers.TAG_FACTORY,
          MessageSerializerTest.SERIALIZER,
          SocketListenerTest.ADDRESS
        )).proxy(PRINTLN_AFTER, new SocketInterceptor("Client"), RemoteTest.CONTRACT_ID_CHECKER, CLIENT_INTERCEPTOR)
      );

    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

}
