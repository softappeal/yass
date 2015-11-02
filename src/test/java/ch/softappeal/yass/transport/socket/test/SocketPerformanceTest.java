package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.Dispatcher;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.test.SerializerTest;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketHelper;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketPerformanceTest extends InvokeTest {

    public static final int COUNTER = 1;

    public static final Serializer PACKET_SERIALIZER = TransportSetup.packetSerializer(SerializerTest.TAGGED_FAST_SERIALIZER);

    private static TransportSetup createSetup(final Executor executor, @Nullable final CountDownLatch latch) {
        return PerformanceTest.createSetup(
            new Dispatcher() {
                @Override public void opened(final Session session, final Runnable runnable) {
                    if (false) {
                        System.out.println("opened: " + session);
                    }
                    executor.execute(runnable);
                }
                @Override public void invoke(final Session session, final Server.Invocation invocation, final Runnable runnable) {
                    if (false) {
                        System.out.println(
                            "invoke: " + session + " " + invocation.oneWay + " " + invocation.method.getName() + " " + Arrays.toString(invocation.arguments) + " " + invocation.service.contractId.id
                        );
                    }
                    runnable.run();
                }
            },
            latch,
            COUNTER
        );
    }

    @Test public void test() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            new SocketTransport(executor, SyncSocketConnection.FACTORY).start(createSetup(executor, null), executor, SocketHelper.ADDRESS);
            final CountDownLatch latch = new CountDownLatch(1);
            new SocketTransport(executor, SyncSocketConnection.FACTORY).connect(createSetup(executor, latch), SocketHelper.ADDRESS);
            latch.await();
            TimeUnit.MILLISECONDS.sleep(100L);
        } finally {
            SocketHelper.shutdown(executor);
        }
    }

}
