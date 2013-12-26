package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Server.ServerInvocation;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.Tunnel;
import ch.softappeal.yass.core.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

public class RemoteTest extends InvokeTest {

  private static int COUNTER;

  public static final Interceptor CONTRACT_ID_CHECKER = new Interceptor() {
    @Override public Object invoke(final Invocation invocation) throws Throwable {
      Assert.assertEquals(TestService.class.getSimpleName(), ContractId.get().id);
      Assert.assertSame(TestService.class, ContractId.get().contract);
      return invocation.proceed();
    }
  };

  private static Interceptor stepInterceptor(final int count) {
    return new Interceptor() {
      @Override public Object invoke(final Invocation invocation) throws Throwable {
        Assert.assertTrue(++COUNTER == count);
        return invocation.proceed();
      }
    };
  }

  private static Client client(final Server server) {
    return new Client(TaggedMethodMapper.FACTORY) {
      @Override public Object invoke(final ClientInvocation clientInvocation) throws Throwable {
        return clientInvocation.invoke(
          Interceptors.composite(
            new Interceptor() {
              @Override public Object invoke(final ch.softappeal.yass.core.Invocation invocation) throws Throwable {
                COUNTER = 0;
                try {
                  return invocation.proceed();
                } finally {
                  Assert.assertTrue(InvokeTest.isOneWay(invocation.method.getName()) || (COUNTER == 4));
                }
              }
            },
            stepInterceptor(1)
          ),
          new Tunnel() {
            @Override public Reply invoke(final Request request) throws Exception {
              Assert.assertTrue(33 == (request.methodId) == clientInvocation.oneWay);
              final ServerInvocation serverInvocation = server.invocation(request);
              Assert.assertTrue(clientInvocation.oneWay == serverInvocation.oneWay);
              try {
                return serverInvocation.invoke(stepInterceptor(3));
              } catch (final Throwable t) {
                if (serverInvocation.oneWay) {
                  println("###", "", t);
                  return null;
                }
                throw t;
              }
            }
          }
        );
      }
    };
  }

  @Test public void test() throws InterruptedException {
    try {
      ContractId.get();
      Assert.fail();
    } catch (final IllegalStateException e) {
      // ignore
    }
    invoke(
      ContractIdTest.ID.invoker(
        client(
          new Server(
            TaggedMethodMapper.FACTORY,
            ContractIdTest.ID.service(new TestServiceImpl(), CONTRACT_ID_CHECKER, stepInterceptor(4), SERVER_INTERCEPTOR)
          )
        )
      ).proxy(CONTRACT_ID_CHECKER, PRINTLN_AFTER, stepInterceptor(2), CLIENT_INTERCEPTOR)
    );
  }

}
