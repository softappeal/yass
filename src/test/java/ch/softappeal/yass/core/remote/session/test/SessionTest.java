package ch.softappeal.yass.core.remote.session.test;

import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.session.Connection;
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

    protected static SessionFactory invokeSessionFactory(final boolean invoke, final boolean createException, final Executor dispatchExecutor) {
        return new SessionFactory() {
            @Override public Session create(final Connection connection) throws Exception {
                return new SimpleSession(connection, dispatchExecutor) {
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
                            ContractIdTest.ID.service(new TestServiceImpl(), invoke ? Interceptors.DIRECT : SERVER_INTERCEPTOR)
                        );
                    }
                    @Override protected void opened() throws Exception {
                        if (createException) {
                            Assert.fail();
                        }
                        println("", "opened", hashCode());
                        if (invoke) {
                            try (Session session = this) {
                                InvokeTest.invoke(session.proxy(ContractIdTest.ID, Interceptors.composite(PRINTLN_AFTER, CLIENT_INTERCEPTOR)));
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
        };
    }

    public interface EchoService {
        byte[] echo(byte[] value);
    }

    private static final class EchoServiceImpl implements EchoService {
        @Override public byte[] echo(final byte[] value) {
            return value;
        }
    }

    private static final ContractId<EchoService> ECHO_ID = ContractId.create(EchoService.class, 0, SimpleMethodMapper.FACTORY);

    protected static SessionFactory performanceSessionFactory(final Executor dispatchExecutor, final @Nullable CountDownLatch latch, final int samples, final int bytes) {
        return new SessionFactory() {
            @Override public Session create(final Connection connection) throws Exception {
                return new Session(connection) {
                    @Override protected Server server() {
                        return new Server(
                            ECHO_ID.service(new EchoServiceImpl())
                        );

                    }
                    @Override protected void dispatchOpened(final Runnable runnable) {
                        dispatchExecutor.execute(runnable);
                    }
                    @Override protected void dispatchServerInvoke(final Server.Invocation invocation, final Runnable runnable) {
                        runnable.run();
                    }
                    @Override public void opened() {
                        if (latch == null) {
                            return;
                        }
                        try (Session session = this) {
                            final EchoService echoService = session.proxy(ECHO_ID);
                            final byte[] value = new byte[bytes];
                            new PerformanceTask() {
                                @Override protected void run(final int count) {
                                    int counter = count;
                                    while (counter-- > 0) {
                                        if (echoService.echo(value).length != bytes) {
                                            throw new RuntimeException();
                                        }
                                    }
                                }
                            }.run(samples, TimeUnit.MICROSECONDS);
                        }
                        latch.countDown();
                    }
                };
            }
        };
    }

    protected static SessionFactory performanceSessionFactory(final Executor dispatchExecutor) {
        return performanceSessionFactory(dispatchExecutor, null, 0, 0);
    }

}
