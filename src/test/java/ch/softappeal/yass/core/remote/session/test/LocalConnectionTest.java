package ch.softappeal.yass.core.remote.session.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.LocalConnection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
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

    private static final Interceptor SESSION_CHECKER = (method, arguments, invocation) -> {
        Assert.assertNotNull(Session.get());
        return invocation.proceed();
    };

    private static TransportSetup createSetup(
        final boolean invoke,
        final Executor dispatcherExecutor,
        final boolean createException,
        final boolean openedException,
        final boolean invokeBeforeOpened
    ) {
        return new TransportSetup(
            new Server(
                TaggedMethodMapper.FACTORY,
                new Service(ContractIdTest.ID, new TestServiceImpl(), invoke ? SESSION_CHECKER : Interceptor.composite(SESSION_CHECKER, SERVER_INTERCEPTOR))
            ),
            dispatcherExecutor,
            PacketSerializerTest.SERIALIZER,
            sessionClient -> {
                if (createException) {
                    throw new Exception("create failed");
                }
                if (invokeBeforeOpened) {
                    sessionClient.proxy(ContractIdTest.ID).nothing();
                }
                return new Session(sessionClient) {
                    @Override public void opened() throws Exception {
                        println("", "opened", hashCode());
                        if (openedException) {
                            throw new Exception("opened failed");
                        }
                        if (invoke) {
                            try (Session session = this) {
                                InvokeTest.invoke(session.proxy(
                                    ContractIdTest.ID,
                                    invoke ? Interceptor.composite(PRINTLN_AFTER, SESSION_CHECKER, CLIENT_INTERCEPTOR) : SESSION_CHECKER
                                ));
                            }
                        }
                    }
                    @Override public void closed(@Nullable final Throwable throwable) {
                        if (invoke) {
                            Assert.assertNull(throwable);
                        }
                        println("", "closed", hashCode() + " " + throwable);
                    }
                };
            }
        );
    }

    public static TransportSetup createSetup(final boolean invoke, final Executor dispatcherExecutor, final boolean createException) {
        return createSetup(invoke, dispatcherExecutor, createException, false, false);
    }

    @Test public void plain() throws InterruptedException {
        Assert.assertNull(Session.get());
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            LocalConnection.connect(createSetup(true, executor, false), createSetup(false, executor, false));
            TimeUnit.MILLISECONDS.sleep(400L);
            System.out.println();
            LocalConnection.connect(createSetup(false, executor, false), createSetup(true, executor, false));
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test public void createException() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            try {
                LocalConnection.connect(createSetup(false, executor, false), createSetup(false, executor, true));
                Assert.fail();
            } catch (final RuntimeException e) {
                Assert.assertEquals(e.getMessage(), "java.lang.Exception: create failed");
            }
        } finally {
            SocketListenerTest.shutdown(executor);
        }
    }

    @Test public void openedException() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            LocalConnection.connect(createSetup(false, executor, false, false, false), createSetup(false, executor, false, true, false));
            TimeUnit.MILLISECONDS.sleep(100L);
        } finally {
            SocketListenerTest.shutdown(executor);
        }
    }

    @Test public void invokeBeforeOpened() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            try {
                LocalConnection.connect(createSetup(true, executor, false, false, true), createSetup(false, executor, false));
                Assert.fail();
            } catch (final NullPointerException e) {
                e.printStackTrace();
            }
        } finally {
            SocketListenerTest.shutdown(executor);
        }
    }

}
