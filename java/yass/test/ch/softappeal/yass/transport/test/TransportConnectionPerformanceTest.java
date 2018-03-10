package ch.softappeal.yass.transport.test;

import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class TransportConnectionPerformanceTest extends TransportTest {

    @Test public void test() throws InterruptedException {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            final var latch = new CountDownLatch(1);
            TransportConnection.connect(performanceTransportSetup(executor, latch, 100, 16), performanceTransportSetup(executor));
            latch.await();
        } finally {
            executor.shutdown();
        }
    }

}
