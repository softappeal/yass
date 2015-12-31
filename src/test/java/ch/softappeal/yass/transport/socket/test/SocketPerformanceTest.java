package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketPerformanceTest extends TransportTest {

    @Test public void test() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        SocketTransport.ListenerCloser listenerCloser = null;
        try {
            listenerCloser = new SocketTransport(executor, SyncSocketConnection.FACTORY).start(transportSetup(executor), executor, SocketHelper.ADDRESS);
            final CountDownLatch latch = new CountDownLatch(1);
            new SocketTransport(executor, SyncSocketConnection.FACTORY).connect(transportSetup(executor, latch, 100), SocketHelper.ADDRESS);
            latch.await();
        } finally {
            SocketHelper.shutdown(listenerCloser, executor);
        }
    }

}
