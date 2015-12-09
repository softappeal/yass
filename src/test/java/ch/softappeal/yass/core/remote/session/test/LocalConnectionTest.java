package ch.softappeal.yass.core.remote.session.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.session.LocalConnection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClosedException;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SimpleSession;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.socket.SocketHelper;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LocalConnectionTest extends InvokeTest {

    private static SessionFactory sessionFactory(
        final boolean invoke,
        final Executor dispatchExecutor,
        final boolean createException,
        final boolean openedException,
        final boolean invokeBeforeOpened
    ) {
        return connection -> {
            if (createException) {
                throw new Exception("create failed");
            }
            return new SimpleSession(connection, dispatchExecutor) {
                {
                    if (invokeBeforeOpened) {
                        proxy(ContractIdTest.ID).nothing();
                    }
                }
                @Override protected Server server() {
                    return new Server(
                        new Service(ContractIdTest.ID, new TestServiceImpl(), invoke ? Interceptor.DIRECT : SERVER_INTERCEPTOR)
                    );
                }
                @Override protected void opened() throws Exception {
                    println("", "opened", hashCode());
                    if (openedException) {
                        throw new Exception("opened failed");
                    }
                    if (invoke) {
                        try (Session session = this) {
                            InvokeTest.invoke(session.proxy(
                                ContractIdTest.ID,
                                invoke ? Interceptor.composite(PRINTLN_AFTER, CLIENT_INTERCEPTOR) : Interceptor.DIRECT
                            ));
                        }
                        try {
                            proxy(ContractIdTest.ID).nothing();
                            Assert.fail();
                        } catch (final SessionClosedException ignored) {
                            // empty
                        }
                    }
                }
                @Override protected void closed(final @Nullable Exception exception) {
                    if (invoke) {
                        Assert.assertNull(exception);
                    }
                    println("", "closed", hashCode() + " " + exception);
                }
            };
        };
    }

    public static SessionFactory sessionFactory(final boolean invoke, final Executor dispatchExecutor, final boolean createException) {
        return sessionFactory(invoke, dispatchExecutor, createException, false, false);
    }

    @Test public void plain() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            LocalConnection.connect(sessionFactory(true, executor, false), sessionFactory(false, executor, false));
            TimeUnit.MILLISECONDS.sleep(400L);
            System.out.println();
            LocalConnection.connect(sessionFactory(false, executor, false), sessionFactory(true, executor, false));
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test public void createException() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            try {
                LocalConnection.connect(sessionFactory(false, executor, false), sessionFactory(false, executor, true));
                Assert.fail();
            } catch (final RuntimeException e) {
                Assert.assertEquals(e.getMessage(), "java.lang.Exception: create failed");
            }
        } finally {
            SocketHelper.shutdown(executor);
        }
    }

    @Test public void openedException() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try {
            LocalConnection.connect(sessionFactory(false, executor, false, false, false), sessionFactory(false, executor, false, true, false));
            TimeUnit.MILLISECONDS.sleep(100L);
        } finally {
            SocketHelper.shutdown(executor);
        }
    }

    @Test public void invokeBeforeOpened() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            try {
                LocalConnection.connect(sessionFactory(true, executor, false, false, true), sessionFactory(false, executor, false));
                Assert.fail();
            } catch (final SessionClosedException e) {
                e.printStackTrace();
            }
        } finally {
            SocketHelper.shutdown(executor);
        }
    }

}
