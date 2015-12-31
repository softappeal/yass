package ch.softappeal.yass.core.remote.session.test;

import ch.softappeal.yass.core.remote.session.LocalConnection;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalConnectionPerformanceTest extends SessionTest {

    @Test public void test() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            LocalConnection.connect(sessionFactory(executor, latch, 100), sessionFactory(executor));
            latch.await();
        } finally {
            executor.shutdown();
        }
    }

}
