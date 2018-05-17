package ch.softappeal.yass.tutorial;

import ch.softappeal.yass.remote.Client;
import ch.softappeal.yass.remote.ContractId;
import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.ClientSetup;
import ch.softappeal.yass.transport.ServerSetup;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.Kt.getTerminate;
import static ch.softappeal.yass.Kt.namedThreadFactory;
import static ch.softappeal.yass.remote.Kt.contractId;
import static ch.softappeal.yass.remote.Kt.getSimpleMethodMapperFactory;
import static ch.softappeal.yass.remote.Kt.service;
import static ch.softappeal.yass.serialize.Kt.getJavaSerializer;
import static ch.softappeal.yass.transport.socket.Kt.socketBinder;
import static ch.softappeal.yass.transport.socket.Kt.socketConnector;
import static ch.softappeal.yass.transport.socket.Kt.socketClient;
import static ch.softappeal.yass.transport.socket.Kt.socketServer;

public class HelloWorld {

    public interface Calculator {
        int add(int a, int b);
    }

    static class CalculatorImpl implements Calculator {
        @Override public int add(int a, int b) {
            return a + b;
        }
    }

    static void useCalculator(Calculator calculator) {
        System.out.println("2 + 3 = " + calculator.add(2, 3));
    }

    @SuppressWarnings("try")
    public static void main(String... args) throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool(namedThreadFactory("hello", getTerminate()));
        try {
            final SocketAddress address = new InetSocketAddress("localhost", 28947);
            final Serializer messageSerializer = getJavaSerializer();
            final ContractId<Calculator> calculatorId = contractId(Calculator.class, 0, getSimpleMethodMapperFactory());
            final Server server = new Server(service(calculatorId, new CalculatorImpl()));
            try (
                AutoCloseable closeable = socketServer(new ServerSetup(server, messageSerializer), executor)
                    .start(executor, socketBinder(address))
            ) {
                final Client client = socketClient(new ClientSetup(messageSerializer), socketConnector(address));
                useCalculator(client.proxy(calculatorId));
            }
        } finally {
            executor.shutdown();
        }
    }

}
