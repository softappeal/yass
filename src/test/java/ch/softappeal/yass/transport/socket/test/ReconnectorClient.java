package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.Link;
import ch.softappeal.yass.core.remote.session.Reconnector;
import ch.softappeal.yass.core.remote.session.SessionClosedException;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ReconnectorClient {

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
            if (testService2.divide(6, 2) != 3) {
                throw new RuntimeException("testService2 failed");
            }
        }
        @Override protected void closed(@Nullable final Throwable throwable) {
            System.out.println("closed");
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
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
        Reconnector.start(
            executor,
            5,
            link.sessionFactory,
            sessionFactory -> SocketTransport.connect(
                new TransportSetup(
                    new Server(
                        TaggedMethodMapper.FACTORY
                    ),
                    executor,
                    PacketSerializerTest.SERIALIZER,
                    sessionFactory
                ),
                executor,
                SocketListenerTest.ADDRESS
            )
        );
        System.out.println("started");
    }

}
