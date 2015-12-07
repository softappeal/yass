package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.session.test.LocalConnectionTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.AsyncSocketConnection;
import ch.softappeal.yass.transport.socket.SocketHelper;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketTransportTest extends InvokeTest {

    private static TransportSetup transportSetup(final boolean invoke, final Executor dispatchExecutor, final boolean createException) {
        return TransportSetup.ofPacketSerializer(JavaSerializer.INSTANCE, LocalConnectionTest.sessionFactory(invoke, dispatchExecutor, createException));
    }

    @Test public void createException() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try {
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .start(transportSetup(false, executor, false), executor, SocketHelper.ADDRESS);
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .connect(transportSetup(false, executor, true), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(200L);
        } finally {
            SocketHelper.shutdown(executor);
        }
    }

    @Test public void clientInvoke() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .start(transportSetup(false, executor, false), executor, SocketHelper.ADDRESS);
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .connect(transportSetup(true, executor, false), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketHelper.shutdown(executor);
        }
    }

    @Test public void clientInvokeAsync() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            new SocketTransport(executor, AsyncSocketConnection.factory(executor, 1))
                .start(transportSetup(false, executor, false), executor, SocketHelper.ADDRESS);
            new SocketTransport(executor, AsyncSocketConnection.factory(executor, 1))
                .connect(transportSetup(true, executor, false), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(1600L);
        } finally {
            SocketHelper.shutdown(executor);
        }
    }

    @Test public void serverInvoke() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .start(transportSetup(true, executor, false), executor, SocketHelper.ADDRESS);
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .connect(transportSetup(false, executor, false), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketHelper.shutdown(executor);
        }
    }

    @Test public void wrongPath() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try {
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .start(new PathResolver(1, transportSetup(true, executor, false)), executor, ServerSocketFactory.getDefault(), SocketHelper.ADDRESS);
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .connect(transportSetup(false, executor, false), 2, SocketFactory.getDefault(), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketHelper.shutdown(executor);
        }
    }

    @Test public void multiplePathes() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        final Integer path1 = 1;
        final Integer path2 = 2;
        final Map<Integer, TransportSetup> pathMappings = new HashMap<>(2);
        pathMappings.put(path1, transportSetup(true, executor, false));
        pathMappings.put(path2, transportSetup(true, executor, false));
        try {
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .start(new PathResolver(pathMappings), executor, ServerSocketFactory.getDefault(), SocketHelper.ADDRESS);
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .connect(transportSetup(false, executor, false), path1, SocketFactory.getDefault(), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
            new SocketTransport(executor, SyncSocketConnection.FACTORY)
                .connect(transportSetup(false, executor, false), path2, SocketFactory.getDefault(), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketHelper.shutdown(executor);
        }
    }

}
