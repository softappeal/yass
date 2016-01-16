package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketPerformanceTest extends TransportTest {

    @Test public void test() throws Exception {
        main(SocketTransportTest.HOSTNAME, String.valueOf(SocketTransportTest.PORT), "100", "16");
    }

    @SuppressWarnings("try")
    public static void main(final String... args) throws Exception {
        if (args.length != 4) {
            throw new RuntimeException("usage: hostname port samples bytes");
        }
        final String hostname = args[0];
        final int port = Integer.valueOf(args[1]);
        final int samples = Integer.valueOf(args[2]);
        final int bytes = Integer.valueOf(args[3]);
        final SocketAddress address = new InetSocketAddress(hostname, port);
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (Closer closer = new SocketTransport(executor, SyncSocketConnection.FACTORY).start(performanceTransportSetup(executor), executor, address)) {
            final CountDownLatch latch = new CountDownLatch(1);
            new SocketTransport(executor, SyncSocketConnection.FACTORY).connect(performanceTransportSetup(executor, latch, samples, bytes), address, 0);
            latch.await();
        } finally {
            executor.shutdown();
        }
    }

}
