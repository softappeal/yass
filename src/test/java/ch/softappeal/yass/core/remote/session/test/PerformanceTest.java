package ch.softappeal.yass.core.remote.session.test;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.LocalConnection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.test.SocketPerformanceTest;
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

    public static final MethodMapper.Factory METHOD_MAPPER_FACTORY = TaggedMethodMapper.FACTORY;

    public static final ContractId<TestService> CONTRACT_ID = ContractId.create(TestService.class, 0);

    public static TransportSetup createSetup(final Executor requestExecutor, @Nullable final CountDownLatch latch, final int samples) {
        return new TransportSetup(
            new Server(METHOD_MAPPER_FACTORY, new Service(CONTRACT_ID, new TestServiceImpl())),
            requestExecutor,
            SocketPerformanceTest.PACKET_SERIALIZER,
            new SessionFactory() {
                @Override public Session create(final SessionClient sessionClient) throws Exception {
                    return new Session(sessionClient) {
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
                                System.out.println("*** oneway");
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
                        @Override public void closed(@Nullable final Throwable throwable) {
                            // empty
                        }
                    };
                }
            }
        );
    }

    @Test public void test() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            LocalConnection.connect(createSetup(executor, latch, COUNTER), createSetup(executor, null, COUNTER));
            latch.await();
        } finally {
            executor.shutdownNow();
        }
    }

}
