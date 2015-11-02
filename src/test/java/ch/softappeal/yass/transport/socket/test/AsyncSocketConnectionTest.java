package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.session.Dispatcher;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.AsyncSocketConnection;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class AsyncSocketConnectionTest {

    public interface Busy {
        @OneWay void busy();
    }

    private static final ContractId<Busy> BUSY_ID = ContractId.create(Busy.class, 0);
    private static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);
    private static final Serializer PACKET_SERIALIZER = JavaSerializer.INSTANCE;
    private static final MethodMapper.Factory METHOD_MAPPER_FACTORY = SimpleMethodMapper.FACTORY;

    public static void main(final String... args) throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("Executor", Exceptions.STD_ERR));

        new SocketTransport(executor, SyncSocketConnection.FACTORY).start(
            new TransportSetup(
                new Server(
                    METHOD_MAPPER_FACTORY,
                    new Service(BUSY_ID, () -> {
                        System.out.println("busy");
                        try {
                            TimeUnit.MILLISECONDS.sleep(1_000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    })
                ),
                new Dispatcher() {
                    @Override public void opened(final Session session, final Runnable runnable) throws Exception {
                        runnable.run();
                    }
                    @Override public void invoke(final Session session, final Server.Invocation invocation, final Runnable runnable) throws Exception {
                        runnable.run();
                    }
                },
                PACKET_SERIALIZER,
                sessionClient -> new Session(sessionClient) {
                    @Override protected void opened() {
                        System.out.println("server opened");
                    }
                    @Override public void closed(@Nullable final Throwable throwable) {
                        System.out.println("server closed");
                    }
                }
            ),
            executor,
            ADDRESS
        );

        new SocketTransport(executor, AsyncSocketConnection.factory(executor, 1_000)).connect(
            new TransportSetup(
                new Server(
                    METHOD_MAPPER_FACTORY
                ),
                executor,
                PACKET_SERIALIZER,
                sessionClient -> new Session(sessionClient) {
                    @Override protected void opened() {
                        System.out.println("client opened");
                        final Busy busy = proxy(BUSY_ID);
                        for (int i = 0; i < 10_000; i++) {
                            busy.busy();
                        }
                        System.out.println("client done");
                    }
                    @Override public void closed(@Nullable final Throwable throwable) {
                        System.out.println("client closed");
                        throwable.printStackTrace(System.out);
                    }
                }
            ),
            ADDRESS
        );

    }

}
