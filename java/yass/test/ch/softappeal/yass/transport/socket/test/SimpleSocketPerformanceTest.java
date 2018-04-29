package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.NamedThreadFactory;
import ch.softappeal.yass.PerformanceTask;
import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.remote.Service;
import ch.softappeal.yass.remote.test.ContractIdTest;
import ch.softappeal.yass.test.InvokeTest;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.test.TransportTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ch.softappeal.yass.transport.socket.test.SimpleSocketTransportTest.delayedShutdown;

public class SimpleSocketPerformanceTest extends TransportTest {

    @SuppressWarnings("try")
    @Test public void test() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (
            var closer = new SimpleSocketTransport(
                executor,
                MESSAGE_SERIALIZER,
                new Server(new Service(ContractIdTest.ID, new InvokeTest.TestServiceImpl()))
            ).start(executor, SocketTransportTest.BINDER)
        ) {
            new PerformanceTask() {
                @Override protected void run(final int count) {
                    final TestService testService = SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketTransportTest.CONNECTOR).proxy(ContractIdTest.ID);
                    var counter = count;
                    while (counter-- > 0) {
                        try {
                            Assert.assertTrue(testService.divide(12, 3) == 4);
                        } catch (final DivisionByZeroException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }.run(1, TimeUnit.MICROSECONDS);
        } finally {
            delayedShutdown(executor);
        }
    }

}
