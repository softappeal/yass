package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Server;
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
        return new Client() {
            @Override public Object invoke(final Client.Invocation clientInvocation) throws Exception {
                COUNTER = 0;
                try {
                    return clientInvocation.invoke(request -> {
                        Assert.assertTrue((33 == request.methodId) == clientInvocation.methodMapping.oneWay);
                        final Server.Invocation serverInvocation = server.invocation(request);
                        Assert.assertTrue(clientInvocation.methodMapping.oneWay == serverInvocation.methodMapping.oneWay);
                        return serverInvocation.invoke();
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
