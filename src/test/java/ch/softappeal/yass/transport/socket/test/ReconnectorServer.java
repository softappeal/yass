package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketHelper;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.transport.socket.SyncSocketConnection;
import ch.softappeal.yass.transport.test.PacketSerializerTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ReconnectorServer {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        new SocketTransport(executor, SyncSocketConnection.FACTORY).start(
            new TransportSetup(
                new Server(
                    TaggedMethodMapper.FACTORY,
                    new Service(ContractIdTest.ID, new InvokeTest.TestServiceImpl())
                ),
                executor,
                PacketSerializerTest.SERIALIZER,
                new SessionFactory() {
                    @Override public Session create(final SessionClient sessionClient) throws Exception {
                        return new Session(sessionClient) {
                            @Override protected void opened() {
                                System.out.println("opened");
                            }
                            @Override public void closed(final @Nullable Throwable throwable) {
                                System.out.println("closed");
                            }
                        };
                    }
                }
            ),
            executor,
            SocketHelper.ADDRESS
        );
        System.out.println("started");
    }

}
