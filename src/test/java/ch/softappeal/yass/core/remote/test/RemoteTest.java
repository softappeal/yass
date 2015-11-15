package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
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
            @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
                Assert.assertTrue(++COUNTER == count);
                return invocation.proceed();
            }
        };
    }

    private static Client client(final Server server) {
        return new Client(TaggedMethodMapper.FACTORY) {
            @Override public Object invoke(final Client.Invocation clientInvocation) throws Throwable {
                return clientInvocation.invoke(
                    Interceptors.composite(
                        new Interceptor() {
                            @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final ch.softappeal.yass.core.Invocation invocation) throws Throwable {
                                COUNTER = 0;
                                try {
                                    return invocation.proceed();
                                } finally {
                                    Assert.assertTrue(COUNTER == 4);
                                }
                            }
                        },
                        stepInterceptor(1)
                    ),
                    new Tunnel() {
                        @Override public Reply invoke(final Request request) throws Exception {
                            Assert.assertTrue((33 == request.methodId) == clientInvocation.oneWay);
                            final Server.Invocation serverInvocation = server.invocation(request);
                            Assert.assertTrue(clientInvocation.oneWay == serverInvocation.oneWay);
                            return serverInvocation.invoke(stepInterceptor(3));
                        }
                    }
                );
            }
        };
    }

    @Test public void test() throws InterruptedException {
        invoke(
            client(new Server(
                TaggedMethodMapper.FACTORY,
                new Service(ContractIdTest.ID, new TestServiceImpl(), stepInterceptor(4), SERVER_INTERCEPTOR)
            )).proxy(ContractIdTest.ID, PRINTLN_AFTER, stepInterceptor(2), CLIENT_INTERCEPTOR)
        );
    }

}
