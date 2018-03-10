package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.transport.DummyPathSerializer;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.PathSerializer;
import ch.softappeal.yass.transport.socket.AsyncSocketConnection;
import ch.softappeal.yass.transport.socket.SocketBinder;
import ch.softappeal.yass.transport.socket.SocketConnector;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ch.softappeal.yass.transport.socket.test.SimpleSocketTransportTest.delayedShutdown;

@SuppressWarnings("try")
public class SocketTransportTest extends TransportTest {

    public static final String HOSTNAME = "localhost";
    public static final int PORT = 28947;
    public static final SocketAddress ADDRESS = new InetSocketAddress(HOSTNAME, PORT);
    public static final SocketConnector CONNECTOR = SocketConnector.create(ADDRESS);
    public static final SocketBinder BINDER = SocketBinder.create(ADDRESS);

    @Test public void clientInvoke() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (var closer = new SocketTransport(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor)).start(executor, BINDER)) {
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(true, false, executor), CONNECTOR);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            delayedShutdown(executor);
        }
    }

    @Test public void clientInvokeAsync() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (var closer = new SocketTransport(executor, AsyncSocketConnection.factory(executor, 1), invokeTransportSetup(false, false, executor)).start(executor, BINDER)) {
            SocketTransport.connect(executor, AsyncSocketConnection.factory(executor, 1), invokeTransportSetup(true, false, executor), CONNECTOR);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            delayedShutdown(executor);
        }
    }

    @Test public void serverInvoke() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (var closer = new SocketTransport(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(true, false, executor)).start(executor, BINDER)) {
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor), CONNECTOR);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            delayedShutdown(executor);
        }
    }

    @Test public void createException() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try (var closer = new SocketTransport(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor)).start(executor, BINDER)) {
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, true, executor), CONNECTOR);
        } finally {
            delayedShutdown(executor);
        }
    }

    @Test public void wrongPath() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try (var closer = new SocketTransport(executor, SyncSocketConnection.FACTORY, PathSerializer.INSTANCE, new PathResolver(1, invokeTransportSetup(true, false, executor))).start(executor, BINDER)) {
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor), CONNECTOR, PathSerializer.INSTANCE, 2);
        } finally {
            delayedShutdown(executor);
        }
    }

    @Test public void multiplePathes() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        final Integer path1 = 1;
        final Integer path2 = 2;
        try (var closer = new SocketTransport(executor, SyncSocketConnection.FACTORY, PathSerializer.INSTANCE, new PathResolver(Map.of(
            path1, invokeTransportSetup(true, false, executor),
            path2, invokeTransportSetup(true, false, executor)
        ))).start(executor, BINDER)) {
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor), CONNECTOR, PathSerializer.INSTANCE, path1);
            TimeUnit.MILLISECONDS.sleep(400L);
            System.out.println();
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor), CONNECTOR, PathSerializer.INSTANCE, path2);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            delayedShutdown(executor);
        }
    }

    @Test public void dummyPathSerializer() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (var closer = new SocketTransport(executor, SyncSocketConnection.FACTORY, DummyPathSerializer.INSTANCE, new PathResolver(DummyPathSerializer.PATH, invokeTransportSetup(true, false, executor))).start(executor, BINDER)) {
            SocketTransport.connect(executor, SyncSocketConnection.FACTORY, invokeTransportSetup(false, false, executor), CONNECTOR, DummyPathSerializer.INSTANCE, 123);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            delayedShutdown(executor);
        }
    }

}
