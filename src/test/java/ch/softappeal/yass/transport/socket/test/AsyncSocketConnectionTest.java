package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SimpleSession;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.AsyncSocketConnection;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class AsyncSocketConnectionTest {

    public interface Busy {
        @OneWay void busy();
    }

    private static final ContractId<Busy> BUSY_ID = ContractId.create(Busy.class, 0, SimpleMethodMapper.FACTORY);
    private static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);
    private static final Serializer PACKET_SERIALIZER = JavaSerializer.INSTANCE;

    public static void main(final String... args) throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("Executor", Exceptions.STD_ERR));

        new SocketTransport(executor, SyncSocketConnection.FACTORY).start(
            TransportSetup.ofPacketSerializer(
                PACKET_SERIALIZER,
                new SessionFactory() {
                    @Override public Session create(final Connection connection) throws Exception {
                        return new SimpleSession(connection, executor) {
                            @Override protected Server server() {
                                return new Server(
                                    BUSY_ID.service(new Busy() {
                                        @Override public void busy() {
                                            System.out.println("busy");
                                            try {
                                                TimeUnit.MILLISECONDS.sleep(1_000);
                                            } catch (InterruptedException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    })
                                );
                            }
                            @Override protected void opened() {
                                System.out.println("acceptor opened");
                            }
                            @Override protected void closed(final boolean exceptional) {
                                System.out.println("acceptor closed: " + exceptional);
                            }
                        };
                    }
                }
            ),
            executor,
            ADDRESS
        );

        new SocketTransport(executor, AsyncSocketConnection.factory(executor, 10)).connect(
            TransportSetup.ofPacketSerializer(
                PACKET_SERIALIZER,
                new SessionFactory() {
                    @Override public Session create(final Connection connection) throws Exception {
                        return new SimpleSession(connection, executor) {
                            @Override protected void opened() {
                                System.out.println("initiator opened");
                                final Busy busy = proxy(BUSY_ID);
                                for (int i = 0; i < 10_000; i++) {
                                    busy.busy();
                                }
                                System.out.println("initiator done");
                            }
                            @Override protected void closed(final boolean exceptional) {
                                System.out.println("initiator closed: " + exceptional);
                            }
                        };
                    }
                }
            ),
            ADDRESS
        );

    }

}
