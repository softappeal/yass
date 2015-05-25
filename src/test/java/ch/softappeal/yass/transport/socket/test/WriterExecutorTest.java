package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.test.SerializerTest;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.PacketSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketConnection;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class WriterExecutorTest {

    public interface StringListener {
        @Tag(1) @OneWay void newString(String value);
    }

    private static final ContractId<StringListener> StringListenerId = ContractId.create(StringListener.class, 0);

    private static final Serializer PACKET_SERIALIZER = new PacketSerializer(new MessageSerializer(SerializerTest.TAGGED_FAST_SERIALIZER));

    private static final MethodMapper.Factory METHOD_MAPPER_FACTORY = TaggedMethodMapper.FACTORY;

    private static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

    private static Executor executor(final String name) {
        return Executors.newCachedThreadPool(new NamedThreadFactory(name, Exceptions.TERMINATE));
    }

    private static void server() {
        SocketTransport.listener(new TransportSetup(
            new Server(METHOD_MAPPER_FACTORY),
            executor("server-dispatcher"),
            PACKET_SERIALIZER,
            sessionClient -> new Session(sessionClient) {
                @Override public void opened() {
                    final SocketConnection socketConnection = (SocketConnection)connection;
                    final StringListener stringListener = proxy(StringListenerId);
                    socketConnection.awaitWriterQueueEmpty();
                    final Executor worker = executor("server-worker");
                    final String s = "hello";
                    for (int i = 0; i < 20; i++) {
                        worker.execute(() -> {
                            while (true) {
                                stringListener.newString(s);
                                try {
                                    TimeUnit.MILLISECONDS.sleep(1);
                                } catch (final InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                }
                @Override public void closed(@Nullable final Throwable throwable) {
                    Exceptions.TERMINATE.uncaughtException(null, throwable);
                }
            }
        )).start(executor("server-listener"), executor("server-socket"), ADDRESS);
    }

    public static void main(final String... args) {
        server();
        final AtomicInteger counter = new AtomicInteger(0);
        SocketTransport.connect(
            new TransportSetup(
                new Server(
                    METHOD_MAPPER_FACTORY,
                    new Service(
                        StringListenerId,
                        value -> {
                            counter.incrementAndGet();
                        }
                    )
                ),
                executor("client-dispatcher"),
                PACKET_SERIALIZER,
                sessionClient -> new Session(sessionClient) {
                    @Override public void closed(@Nullable final Throwable throwable) {
                        Exceptions.TERMINATE.uncaughtException(null, throwable);
                    }
                }
            ),
            executor("client-socket"),
            ADDRESS
        );
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
            () -> System.out.println(counter.get()),
            0,
            1,
            TimeUnit.SECONDS
        );
    }

}
