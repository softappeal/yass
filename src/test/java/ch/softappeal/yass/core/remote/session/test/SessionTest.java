package ch.softappeal.yass.core.remote.session.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClosedException;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SimpleSession;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.PerformanceTask;
import org.junit.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public abstract class SessionTest extends InvokeTest {

    protected static SessionFactory sessionFactory(final boolean invoke, final boolean createException, final Executor dispatchExecutor) {
        return connection -> new SimpleSession(connection, dispatchExecutor) {
            {
                if (createException) {
                    throw new Exception("create failed");
                }
                Assert.assertTrue(isClosed());
                try {
                    proxy(ContractIdTest.ID).nothing();
                    Assert.fail();
                } catch (final SessionClosedException ignore) {
                    // empty
                }
            }
            @Override protected Server server() {
                if (createException) {
                    Assert.fail();
                }
                Assert.assertFalse(isClosed());
                return new Server(
                    ContractIdTest.ID.service(new TestServiceImpl(), invoke ? Interceptor.DIRECT : SERVER_INTERCEPTOR)
                );
            }
            @Override protected void opened() throws Exception {
                if (createException) {
                    Assert.fail();
                }
                println("", "opened", hashCode());
                if (invoke) {
                    try (Session session = this) {
                        InvokeTest.invoke(session.proxy(ContractIdTest.ID, Interceptor.composite(PRINTLN_AFTER, CLIENT_INTERCEPTOR)));
                    }
                    try {
                        proxy(ContractIdTest.ID).nothing();
                        Assert.fail();
                    } catch (final SessionClosedException ignore) {
                        // empty
                    }
                }
            }
            @Override protected void closed(final @Nullable Exception exception) {
                if (createException) {
                    Assert.fail();
                }
                Assert.assertTrue(isClosed());
                println("", "closed", hashCode() + " " + exception);
            }
        };
    }

    public static final ContractId<TestService> CONTRACT_ID = ContractId.create(TestService.class, 0, TaggedMethodMapper.FACTORY);

    protected static SessionFactory sessionFactory(final Executor dispatchExecutor, final @Nullable CountDownLatch latch, final int samples) {
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

    protected static SessionFactory sessionFactory(final Executor dispatchExecutor) {
        return sessionFactory(dispatchExecutor, null, 0);
    }

}
