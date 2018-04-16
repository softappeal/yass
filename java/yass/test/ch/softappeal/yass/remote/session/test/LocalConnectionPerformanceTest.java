package ch.softappeal.yass.remote.session.test;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.NamedThreadFactory;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class LocalConnectionPerformanceTest extends SessionTest {

    @Test public void test() throws InterruptedException {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            final var latch = new CountDownLatch(1);
            LocalConnection.connect(performanceSessionFactory(executor, latch, 100, 16), performanceSessionFactory(executor));
            latch.await();
        } finally {
            executor.shutdown();
        }
    }

}
