package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleSocketTransportTest extends TransportTest {

    private static Interceptor socketInterceptor(final String side) {
        return new Interceptor() {
            @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Exception {
                System.out.println(side + ": " + Check.notNull(SimpleSocketTransport.socket()));
                return invocation.proceed();
            }
        };
    }

    @SuppressWarnings("try")
    @Test public void test() throws Exception {
        Assert.assertNull(SimpleSocketTransport.socket());
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (
            Closer closer = new SimpleSocketTransport(executor)
                .start(
                    new Server(
                        ContractIdTest.ID.service(
                            new TestServiceImpl(),
                            socketInterceptor("server"),
                            SERVER_INTERCEPTOR
                        )
                    ),
                    MESSAGE_SERIALIZER,
                    executor,
                    SocketTransportTest.ADDRESS
                )
        ) {
            invoke(
                SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketTransportTest.ADDRESS)
                    .proxy(
                        ContractIdTest.ID,
                        socketInterceptor("client"),
                        CLIENT_INTERCEPTOR
                    )
            );
        } finally {
            executor.shutdown();
        }
    }

}
