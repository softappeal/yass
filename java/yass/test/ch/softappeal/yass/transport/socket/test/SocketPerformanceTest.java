package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.NamedThreadFactory;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.transport.test.TransportTest;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.transport.socket.test.SimpleSocketTransportTest.delayedShutdown;

public class SocketPerformanceTest extends TransportTest {

    @SuppressWarnings("try")
    @Test public void test() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (
            var closer = new SocketTransport(
                executor,
                SyncSocketConnection.FACTORY,
                performanceTransportSetup(executor)
            ).start(executor, SocketTransportTest.BINDER)
        ) {
            final var latch = new CountDownLatch(1);
            SocketTransport.connect(
                executor,
                SyncSocketConnection.FACTORY,
                performanceTransportSetup(executor, latch, 100, 16),
                SocketTransportTest.CONNECTOR
            );
            latch.await();
        } finally {
            delayedShutdown(executor);
        }
    }

}
