package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.PathSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.AsyncSocketConnection;
import ch.softappeal.yass.transport.socket.SimpleSocketConnector;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.util.Closer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("try")
public class SocketTransportTest extends TransportTest {

    public static final String HOSTNAME = "localhost";
    public static final int PORT = 28947;
    public static final SocketAddress ADDRESS = new InetSocketAddress(HOSTNAME, PORT);

    @Test public void clientInvoke() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (Closer closer = new SocketTransport(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor)).start(executor, ADDRESS)) {
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(true, false, executor), new SimpleSocketConnector(ADDRESS));
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            executor.shutdown();
        }
    }

    @Test public void clientInvokeAsync() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (Closer closer = new SocketTransport(executor, AsyncSocketConnection.factory(executor, 1), invokeTransportSetup(false, false, executor)).start(executor, ADDRESS)) {
            SocketTransport.connect(executor, AsyncSocketConnection.factory(executor, 1), invokeTransportSetup(true, false, executor), new SimpleSocketConnector(ADDRESS));
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            executor.shutdown();
        }
    }

    @Test public void serverInvoke() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (Closer closer = new SocketTransport(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(true, false, executor)).start(executor, ADDRESS)) {
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor), new SimpleSocketConnector(ADDRESS));
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            executor.shutdown();
        }
    }

    @Test public void createException() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try (Closer closer = new SocketTransport(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor)).start(executor, ADDRESS)) {
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, true, executor), new SimpleSocketConnector(ADDRESS));
            TimeUnit.MILLISECONDS.sleep(100L);
        } finally {
            executor.shutdown();
        }
    }

    @Test public void wrongPath() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try (Closer closer = new SocketTransport(executor, SyncSocketConnection.FACTORY, PathSerializer.INSTANCE, new PathResolver(1, invokeTransportSetup(true, false, executor))).start(executor, ADDRESS)) {
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor), new SimpleSocketConnector(ADDRESS), PathSerializer.INSTANCE, 2);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            executor.shutdown();
        }
    }

    @Test public void multiplePathes() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        final Integer path1 = 1;
        final Integer path2 = 2;
        final Map<Integer, TransportSetup> pathMappings = new HashMap<>(2);
        pathMappings.put(path1, invokeTransportSetup(true, false, executor));
        pathMappings.put(path2, invokeTransportSetup(true, false, executor));
        try (Closer closer = new SocketTransport(executor, SyncSocketConnection.FACTORY, PathSerializer.INSTANCE, new PathResolver(pathMappings)).start(executor, ADDRESS)) {
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor), new SimpleSocketConnector(ADDRESS), PathSerializer.INSTANCE, path1);
            TimeUnit.MILLISECONDS.sleep(400L);
            System.out.println();
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor), new SimpleSocketConnector(ADDRESS), PathSerializer.INSTANCE, path2);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            executor.shutdown();
        }
    }

}
