package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.DummyPathSerializer;
import ch.softappeal.yass.transport.PathResolver;
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

public class ReconnectorServer {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        SocketTransport.listener(
            DummyPathSerializer.INSTANCE,
            new PathResolver(
                DummyPathSerializer.PATH,
                new TransportSetup(
                    new Server(
                        TaggedMethodMapper.FACTORY,
                        new Service(ContractIdTest.ID, new InvokeTest.TestServiceImpl())
                    ),
                    executor,
                    PacketSerializerTest.SERIALIZER,
                    sessionClient -> new Session(sessionClient) {
                        @Override protected void opened() {
                            System.out.println("opened");
                        }
                        @Override public void closed(@Nullable final Throwable throwable) {
                            System.out.println("closed");
                        }
                    }
                )
            )
        ).start(executor, new SocketExecutor(executor, Exceptions.STD_ERR), SocketListenerTest.ADDRESS);
        System.out.println("started");
    }

}
