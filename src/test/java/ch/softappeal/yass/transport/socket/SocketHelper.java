package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.util.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;

public class SocketHelper {

    public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

    public static void shutdown(final SocketTransport.@Nullable ListenerCloser listenerCloser, final ExecutorService executorService) {
        if (listenerCloser != null) {
            listenerCloser.close();
        }
        executorService.shutdownNow();
    }

}
