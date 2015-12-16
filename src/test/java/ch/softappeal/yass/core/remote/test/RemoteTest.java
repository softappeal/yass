package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Tunnel;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class RemoteTest extends InvokeTest {

    private static int COUNTER;

    private static Interceptor stepInterceptor(final int count) {
        return new Interceptor() {
            @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Exception {
                Assert.assertTrue(++COUNTER == count);
                return invocation.proceed();
            }
        };
    }

    private static Client client(final Server server) {
        return new Client() {
            @Override public Object invoke(final Client.Invocation clientInvocation) throws Exception {
                COUNTER = 0;
                try {
                    return clientInvocation.invoke(new Tunnel() {
                        @Override public Reply invoke(final Request request) throws Exception {
                            Assert.assertTrue((33 == request.methodId) == clientInvocation.methodMapping.oneWay);
                            final Server.Invocation serverInvocation = server.invocation(request);
                            Assert.assertTrue(clientInvocation.methodMapping.oneWay == serverInvocation.methodMapping.oneWay);
                            return serverInvocation.invoke();
                        }
                    });
                } finally {
                    Assert.assertTrue(COUNTER == 2);
                }
            }
        };
    }

    @Test public void test() throws InterruptedException {
        invoke(
            client(new Server(
                ContractIdTest.ID.service(new TestServiceImpl(), stepInterceptor(2), SERVER_INTERCEPTOR)
            )).proxy(ContractIdTest.ID, PRINTLN_AFTER, stepInterceptor(1), CLIENT_INTERCEPTOR)
        );
    }

}
