package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.AsyncSocketConnection;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketTransportTest extends TransportTest {

    @Test public void clientInvoke() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        SocketTransport.ListenerCloser listenerCloser = null;
        try {
            listenerCloser = new SocketTransport(executor, SyncSocketConnection.FACTORY).start(invokeTransportSetup(false, false, executor), executor, SocketHelper.ADDRESS);
            new SocketTransport(executor, SyncSocketConnection.FACTORY).connect(invokeTransportSetup(true, false, executor), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketHelper.shutdown(listenerCloser, executor);
        }
    }

    @Test public void clientInvokeAsync() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        SocketTransport.ListenerCloser listenerCloser = null;
        try {
            listenerCloser = new SocketTransport(executor, AsyncSocketConnection.factory(executor, 1)).start(invokeTransportSetup(false, false, executor), executor, SocketHelper.ADDRESS);
            new SocketTransport(executor, AsyncSocketConnection.factory(executor, 1)).connect(invokeTransportSetup(true, false, executor), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(2500L);
        } finally {
            SocketHelper.shutdown(listenerCloser, executor);
        }
    }

    @Test public void serverInvoke() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        SocketTransport.ListenerCloser listenerCloser = null;
        try {
            listenerCloser = new SocketTransport(executor, SyncSocketConnection.FACTORY).start(invokeTransportSetup(true, false, executor), executor, SocketHelper.ADDRESS);
            new SocketTransport(executor, SyncSocketConnection.FACTORY).connect(invokeTransportSetup(false, false, executor), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketHelper.shutdown(listenerCloser, executor);
        }
    }

    @Test public void createException() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        SocketTransport.ListenerCloser listenerCloser = null;
        try {
            listenerCloser = new SocketTransport(executor, SyncSocketConnection.FACTORY).start(invokeTransportSetup(false, false, executor), executor, SocketHelper.ADDRESS);
            new SocketTransport(executor, SyncSocketConnection.FACTORY).connect(invokeTransportSetup(false, true, executor), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(100L);
        } finally {
            SocketHelper.shutdown(listenerCloser, executor);
        }
    }

    @Test public void wrongPath() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        SocketTransport.ListenerCloser listenerCloser = null;
        try {
            listenerCloser = new SocketTransport(executor, SyncSocketConnection.FACTORY).start(new PathResolver(1, invokeTransportSetup(true, false, executor)), executor, ServerSocketFactory.getDefault(), SocketHelper.ADDRESS);
            new SocketTransport(executor, SyncSocketConnection.FACTORY).connect(invokeTransportSetup(false, false, executor), 2, SocketFactory.getDefault(), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketHelper.shutdown(listenerCloser, executor);
        }
    }

    @Test public void multiplePathes() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        SocketTransport.ListenerCloser listenerCloser = null;
        final Integer path1 = 1;
        final Integer path2 = 2;
        final Map<Integer, TransportSetup> pathMappings = new HashMap<>(2);
        pathMappings.put(path1, invokeTransportSetup(true, false, executor));
        pathMappings.put(path2, invokeTransportSetup(true, false, executor));
        try {
            listenerCloser = new SocketTransport(executor, SyncSocketConnection.FACTORY).start(new PathResolver(pathMappings), executor, ServerSocketFactory.getDefault(), SocketHelper.ADDRESS);
            new SocketTransport(executor, SyncSocketConnection.FACTORY).connect(invokeTransportSetup(false, false, executor), path1, SocketFactory.getDefault(), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
            System.out.println();
            new SocketTransport(executor, SyncSocketConnection.FACTORY).connect(invokeTransportSetup(false, false, executor), path2, SocketFactory.getDefault(), SocketHelper.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketHelper.shutdown(listenerCloser, executor);
        }
    }

}
