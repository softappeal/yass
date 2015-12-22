package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.transport.socket.SocketTransport;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class SocketHelper {

    public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

    public static void shutdown(final SocketTransport.ListenerCloser listenerCloser, final ExecutorService executorService) throws InterruptedException {
        listenerCloser.close();
        TimeUnit.MILLISECONDS.sleep(100L);
        executorService.shutdownNow();
    }

}
