package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.transport.PathSerializer;
import ch.softappeal.yass.transport.SimplePathResolver;
import ch.softappeal.yass.transport.SimpleTransportSetup;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("try")
public class SimpleSocketTransportTest extends TransportTest {

    public static void delayedShutdown(final ExecutorService executor) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100L); // gives client time to finish socket close
        executor.shutdown();
    }

    private static Interceptor socketInterceptor(final String side) {
        return (method, arguments, invocation) -> {
            System.out.println(side + ": " + SimpleSocketTransport.socket());
            return invocation.proceed();
        };
    }

    @Test public void invoke() throws Exception {
        try {
            SimpleSocketTransport.socket();
            Assert.fail();
        } catch (final IllegalStateException e) {
            System.out.println(e);
        }
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (
            var closer = new SimpleSocketTransport(
                executor,
                MESSAGE_SERIALIZER,
                new Server(
                    ContractIdTest.ID.service(
                        new TestServiceImpl(),
                        socketInterceptor("server"),
                        SERVER_INTERCEPTOR
                    )
                )
            ).start(executor, SocketTransportTest.BINDER)
        ) {
            invoke(
                SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketTransportTest.CONNECTOR)
                    .proxy(
                        ContractIdTest.ID,
                        socketInterceptor("client"),
                        CLIENT_INTERCEPTOR
                    )
            );
        } finally {
            delayedShutdown(executor);
        }
    }

    @Test public void wrongPath() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try (
            var closer = new SimpleSocketTransport(
                executor,
                MESSAGE_SERIALIZER,
                new Server(ECHO_ID.service(new EchoServiceImpl()))
            ).start(executor, SocketTransportTest.BINDER)
        ) {
            try {
                SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketTransportTest.CONNECTOR, PathSerializer.INSTANCE, 123)
                    .proxy(ECHO_ID).echo(null);
                Assert.fail();
            } catch (final RuntimeException ignore) {
                // empty
            }
        } finally {
            delayedShutdown(executor);
        }
    }

    @Test public void multiplePathes() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        final Integer path1 = 1;
        final Integer path2 = 2;
        try (
            var closer = new SimpleSocketTransport(
                executor,
                PathSerializer.INSTANCE,
                new SimplePathResolver(Map.of(
                    path1, new SimpleTransportSetup(MESSAGE_SERIALIZER, new Server(ECHO_ID.service(new EchoServiceImpl(), (method, arguments, invocation) -> {
                        System.out.println("path 1");
                        return invocation.proceed();
                    }))),
                    path2, new SimpleTransportSetup(MESSAGE_SERIALIZER, new Server(ECHO_ID.service(new EchoServiceImpl(), (method, arguments, invocation) -> {
                        System.out.println("path 2");
                        return invocation.proceed();
                    })))
                ))
            ).start(executor, SocketTransportTest.BINDER)
        ) {
            SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketTransportTest.CONNECTOR, PathSerializer.INSTANCE, path1)
                .proxy(ECHO_ID).echo(null);
            SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketTransportTest.CONNECTOR, PathSerializer.INSTANCE, path2)
                .proxy(ECHO_ID).echo(null);
        } finally {
            delayedShutdown(executor);
        }
    }

}
