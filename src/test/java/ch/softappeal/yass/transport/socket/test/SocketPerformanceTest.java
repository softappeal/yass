package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.session.test.PerformanceTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketHelper;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketPerformanceTest extends InvokeTest {

    public static final int COUNTER = 1;

    @Test public void test() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .start(TransportSetup.ofPacketSerializer(JavaSerializer.INSTANCE, PerformanceTest.sessionFactory(executor, null, COUNTER)), executor, SocketHelper.ADDRESS);
            final CountDownLatch latch = new CountDownLatch(1);
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .connect(TransportSetup.ofPacketSerializer(JavaSerializer.INSTANCE, PerformanceTest.sessionFactory(executor, latch, COUNTER)), SocketHelper.ADDRESS);
            latch.await();
            TimeUnit.MILLISECONDS.sleep(100L);
        } finally {
            SocketHelper.shutdown(executor);
        }
    }

}
