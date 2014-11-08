package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Server.ServerInvocation;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

public class RemoteTest extends InvokeTest {

    private static int COUNTER;

    private static Interceptor stepInterceptor(final int count) {
        return (method, arguments, invocation) -> {
            Assert.assertTrue(++COUNTER == count);
            return invocation.proceed();
        };
    }

    private static Client client(final Server server) {
        return new Client(TaggedMethodMapper.FACTORY) {
            @Override public Object invoke(final ClientInvocation clientInvocation) throws Throwable {
                return clientInvocation.invoke(
                    Interceptor.composite(
                        (method, arguments, invocation) -> {
                            COUNTER = 0;
                            try {
                                return invocation.proceed();
                            } finally {
                                Assert.assertTrue(COUNTER == 4);
                            }
                        },
                        stepInterceptor(1)
                    ),
                    request -> {
                        Assert.assertTrue((33 == (Integer)request.methodId) == clientInvocation.oneWay);
                        final ServerInvocation serverInvocation = server.invocation(request);
                        Assert.assertTrue(clientInvocation.oneWay == serverInvocation.oneWay);
                        return serverInvocation.invoke(stepInterceptor(3));
                    }
                );
            }
        };
    }

    @Test public void test() throws InterruptedException {
        invoke(
            client(
                new Server(
                    TaggedMethodMapper.FACTORY,
                    new Service(ContractIdTest.ID, new TestServiceImpl(), stepInterceptor(4), SERVER_INTERCEPTOR)
                )
            ).invoker(ContractIdTest.ID).proxy(PRINTLN_AFTER, stepInterceptor(2), CLIENT_INTERCEPTOR)
        );
    }

}
