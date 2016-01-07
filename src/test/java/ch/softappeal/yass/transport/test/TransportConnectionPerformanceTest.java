package ch.softappeal.yass.transport.test;

import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransportConnectionPerformanceTest extends TransportTest {

    @Test public void test() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            TransportConnection.connect(transportSetup(executor, latch, 100), transportSetup(executor));
            latch.await();
        } finally {
            executor.shutdown();
        }
    }

}
