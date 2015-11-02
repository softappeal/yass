package ch.softappeal.yass.transport.socket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class SocketHelper {

    public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

    public static void shutdown(final ExecutorService executorService) throws InterruptedException {
        executorService.shutdownNow();
        TimeUnit.MILLISECONDS.sleep(3 * SocketTransport.ACCEPT_TIMEOUT_MILLISECONDS);
    }

}
