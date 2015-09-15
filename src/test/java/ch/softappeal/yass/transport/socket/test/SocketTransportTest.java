package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.session.test.LocalConnectionTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.PathResolver;
import ch.softappeal.yass.transport.PathSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import javax.net.SocketFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketTransportTest extends InvokeTest {

    @Test public void createException() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try {
            SocketTransport.listener(LocalConnectionTest.createSetup(false, executor, false)).start(executor, executor, SocketListenerTest.ADDRESS);
            SocketTransport.connect(LocalConnectionTest.createSetup(false, executor, true), executor, SocketListenerTest.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(200L);
        } finally {
            SocketListenerTest.shutdown(executor);
        }
    }

    @Test public void clientInvoke() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            SocketTransport.listener(LocalConnectionTest.createSetup(false, executor, false)).start(executor, executor, SocketListenerTest.ADDRESS);
            SocketTransport.connect(LocalConnectionTest.createSetup(true, executor, false), executor, SocketListenerTest.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketListenerTest.shutdown(executor);
        }
    }

    @Test public void serverInvoke() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            SocketTransport.listener(LocalConnectionTest.createSetup(true, executor, false)).start(executor, executor, SocketListenerTest.ADDRESS);
            SocketTransport.connect(LocalConnectionTest.createSetup(false, executor, false), executor, SocketListenerTest.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketListenerTest.shutdown(executor);
        }
    }

    @Test public void wrongPath() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try {
            SocketTransport.listener(PathSerializer.INSTANCE, new PathResolver(1, LocalConnectionTest.createSetup(true, executor, false))).start(executor, executor, SocketListenerTest.ADDRESS);
            SocketTransport.connect(LocalConnectionTest.createSetup(false, executor, false), executor, PathSerializer.INSTANCE, 2, SocketFactory.getDefault(), SocketListenerTest.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketListenerTest.shutdown(executor);
        }
    }

    @Test public void multiplePathes() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        final Integer path1 = 1;
        final Integer path2 = 2;
        final Map<Integer, TransportSetup> pathMappings = new HashMap<>(2);
        pathMappings.put(path1, LocalConnectionTest.createSetup(true, executor, false));
        pathMappings.put(path2, LocalConnectionTest.createSetup(true, executor, false));
        try {
            SocketTransport.listener(PathSerializer.INSTANCE, new PathResolver(pathMappings)).start(executor, executor, SocketListenerTest.ADDRESS);
            SocketTransport.connect(LocalConnectionTest.createSetup(false, executor, false), executor, PathSerializer.INSTANCE, path1, SocketFactory.getDefault(), SocketListenerTest.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
            SocketTransport.connect(LocalConnectionTest.createSetup(false, executor, false), executor, PathSerializer.INSTANCE, path2, SocketFactory.getDefault(), SocketListenerTest.ADDRESS);
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            SocketListenerTest.shutdown(executor);
        }
    }

}
