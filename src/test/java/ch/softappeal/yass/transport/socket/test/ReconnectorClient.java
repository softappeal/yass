package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.Dispatcher;
import ch.softappeal.yass.core.remote.session.Link;
import ch.softappeal.yass.core.remote.session.Reconnector;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClosedException;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketHelper;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Stopwatch;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ReconnectorClient {

    private static void loop(final InvokeTest.TestService testService) throws InvokeTest.DivisionByZeroException {
        while (true) {
            final Stopwatch stopwatch = new Stopwatch();
            for (int i = 0; i < 10_000; i++) {
                if (testService.divide(6, 2) != 3) {
                    throw new RuntimeException("testService2 failed");
                }
            }
            stopwatch.stop();
            System.out.println(stopwatch.milliSeconds() + "ms");
        }
    }

    private static final class TestLink extends Link {
        public final InvokeTest.TestService testService1 = proxy(ContractIdTest.ID);
        public final InvokeTest.TestService testService2 = proxy(ContractIdTest.ID);
        @Override protected void opened() throws InvokeTest.DivisionByZeroException {
            System.out.println("opened");
            if (!up() || !(session().link instanceof TestLink)) {
                throw new RuntimeException("wrong link");
            }
            if (testService1.divide(6, 3) != 2) {
                throw new RuntimeException("testService1 failed");
            }
            loop(testService2);
        }
        @Override protected void closed(final @Nullable Throwable throwable) {
            System.out.println("closed");
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }

    private static void connect(final Executor executor, final SessionFactory sessionFactory) {
        new SocketTransport(executor, SyncSocketConnection.FACTORY).connect(
            new TransportSetup(
                new Server(
                    TaggedMethodMapper.FACTORY
                ),
                new Dispatcher() {
                    @Override public void opened(final Session session, final Runnable runnable) {
                        executor.execute(runnable);
                    }
                    @Override public void invoke(final Session session, final Server.Invocation invocation, final Runnable runnable) {
                        runnable.run();
                    }
                },
                PacketSerializerTest.SERIALIZER,
                sessionFactory
            ),
            SocketHelper.ADDRESS
        );
    }

    public static void main(final String... args) throws Exception {
        final TestLink link = new TestLink();
        if (link.up() || (link.session() != null)) {
            throw new RuntimeException("link is up");
        }
        try {
            link.testService1.divide(100, 10);
            throw new Exception("session is up");
        } catch (final SessionClosedException ignore) {
            // empty
        }
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        if (true) {
            Reconnector.start(
                executor,
                5,
                link.sessionFactory,
                sessionFactory -> connect(executor, sessionFactory)
            );
        } else {
            connect(
                executor,
                sessionClient -> new Session(sessionClient) {
                    @Override protected void opened() throws InvokeTest.DivisionByZeroException {
                        System.out.println("opened");
                        loop(proxy(ContractIdTest.ID));
                    }
                    @Override protected void closed(final @Nullable Throwable throwable) {
                        System.out.println("closed");
                        if (throwable != null) {
                            throwable.printStackTrace(System.out);
                        }
                    }
                }
            );
        }
        System.out.println("started");
    }

}
