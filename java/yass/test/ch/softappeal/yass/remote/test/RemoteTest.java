package ch.softappeal.yass.remote.test;

import ch.softappeal.yass.Interceptor;
import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.remote.Client;
import ch.softappeal.yass.remote.ContractId;
import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.remote.Service;
import ch.softappeal.yass.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

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
            @Override protected Object invokeSync(final ContractId<?> contractId, final Interceptor interceptor, final Method method, final @Nullable Object[] arguments) throws Exception {
                COUNTER = 0;
                try {
                    return super.invokeSync(contractId, interceptor, method, arguments);
                } finally {
                    Assert.assertTrue(COUNTER == 2);
                }
            }
            @Override public void invoke(final Client.Invocation invocation) throws Exception {
                invocation.invoke(false, request -> {
                    Assert.assertTrue((33 == request.methodId) == invocation.methodMapping.oneWay);
                    final var serverInvocation = server.invocation(false, request);
                    Assert.assertTrue(invocation.methodMapping.oneWay == serverInvocation.methodMapping.oneWay);
                    serverInvocation.invoke(invocation::settle);
                });
            }
        };
    }

    @Test public void test() throws InterruptedException {
        invoke(
            client(new Server(
                new Service(ContractIdTest.ID, new TestServiceImpl(), stepInterceptor(2), SERVER_INTERCEPTOR)
            )).proxy(ContractIdTest.ID, PRINTLN_AFTER, stepInterceptor(1), CLIENT_INTERCEPTOR)
        );
    }

}
