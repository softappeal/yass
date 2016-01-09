package ch.softappeal.yass.transport.socket.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.test.TransportTest;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleSocketTransportTest extends TransportTest {

    private static final Serializer MESSAGE_SERIALIZER = new MessageSerializer(CONTRACT_SERIALIZER);

    @SuppressWarnings("try")
    @Test public void test() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try (
            AutoCloseable closer = new SimpleSocketTransport(executor)
                .start(
                    new Server(ContractIdTest.ID.service(new TestServiceImpl(), SERVER_INTERCEPTOR)),
                    MESSAGE_SERIALIZER,
                    executor,
                    SocketTransportTest.ADDRESS
                )
        ) {
            invoke(
                SimpleSocketTransport.client(MESSAGE_SERIALIZER, SocketTransportTest.ADDRESS)
                    .proxy(ContractIdTest.ID, CLIENT_INTERCEPTOR)
            );
        } finally {
            executor.shutdown();
        }
    }

}
