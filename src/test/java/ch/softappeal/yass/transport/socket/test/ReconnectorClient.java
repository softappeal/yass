package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.Reconnector;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.transport.DummyPathSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketExecutor;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ReconnectorClient {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        Reconnector.start(
            executor,
            5,
            sessionClient -> new Session(sessionClient) {
                @Override protected void opened() throws Exception {
                    System.out.println("opened");
                    if (proxy(ContractIdTest.ID).divide(6, 3) != 2) {
                        throw new RuntimeException();
                    }
                }
                @Override public void closed(@Nullable final Throwable throwable) {
                    System.out.println("closed");
                }
            },
            sessionFactory -> SocketTransport.connect(
                new TransportSetup(
                    new Server(
                        TaggedMethodMapper.FACTORY
                    ),
                    executor,
                    PacketSerializerTest.SERIALIZER,
                    sessionFactory
                ),
                new SocketExecutor(executor, Exceptions.STD_ERR),
                DummyPathSerializer.INSTANCE, DummyPathSerializer.PATH,
                SocketListenerTest.ADDRESS
            )
        );
        System.out.println("started");
    }

}
