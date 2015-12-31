package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.transport.socket.SocketTransport;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;

public class SocketHelper {

    public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

    public static void shutdown(final SocketTransport.ListenerCloser listenerCloser, final ExecutorService executorService) {
        listenerCloser.close();
        executorService.shutdown();
    }

}
