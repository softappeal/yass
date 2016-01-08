package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.transport.socket.SocketTransport;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;

public class SocketHelper {

    public static final String HOSTNAME = "localhost";
    public static final int PORT = 28947;
    public static final SocketAddress ADDRESS = new InetSocketAddress(HOSTNAME, PORT);

    public static void shutdown(final SocketTransport.ListenerCloser listenerCloser, final ExecutorService executorService) {
        listenerCloser.close();
        executorService.shutdown();
    }

}
