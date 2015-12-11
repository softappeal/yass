package ch.softappeal.yass.core.remote.session.test;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.LocalConnection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SimpleSession;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.PerformanceTask;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PerformanceTest extends InvokeTest {

    private static final int COUNTER = 100;

    public static final ContractId<TestService> CONTRACT_ID = ContractId.create(TestService.class, 0, TaggedMethodMapper.FACTORY);

    public static SessionFactory sessionFactory(final Executor dispatchExecutor, final @Nullable CountDownLatch latch, final int samples) {
        return connection -> new SimpleSession(connection, dispatchExecutor) {
            @Override protected Server server() {
                return new Server(
                    CONTRACT_ID.service(new TestServiceImpl())
                );

            }
            @Override public void opened() {
                if (latch == null) {
                    return;
                }
                try (Session session = this) {
                    final TestService testService = session.proxy(CONTRACT_ID);
                    System.out.println("*** rpc");
                    new PerformanceTask() {
                        @Override protected void run(final int count) throws DivisionByZeroException {
                            int counter = count;
                            while (counter-- > 0) {
                                Assert.assertTrue(testService.divide(12, 4) == 3);
                            }
                        }
                    }.run(samples, TimeUnit.MICROSECONDS);
                    System.out.println("*** oneWay");
                    new PerformanceTask() {
                        @Override protected void run(final int count) {
                            int counter = count;
                            while (counter-- > 0) {
                                testService.oneWay(-1);
                            }
                        }
                    }.run(samples, TimeUnit.MICROSECONDS);
                }
                latch.countDown();
            }
        };
    }

    @Test public void test() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            LocalConnection.connect(sessionFactory(executor, latch, COUNTER), sessionFactory(executor, null, COUNTER));
            latch.await();
        } finally {
            executor.shutdownNow();
        }
    }

}
