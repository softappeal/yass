package ch.softappeal.yass.tutorial;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.socket.SocketBinder;
import ch.softappeal.yass.transport.socket.SocketConnector;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class HelloWorld {

    private static final Executor EXECUTOR = Executors.newCachedThreadPool();

    private static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

    private static final Serializer SERIALIZER = JavaSerializer.INSTANCE;

    private static final MethodMapper.Factory METHOD_MAPPER_FACTORY = SimpleMethodMapper.FACTORY;

    public interface Calculator {
        int add(int a, int b);
    }

    static class CalculatorImpl implements Calculator {
        public int add(int a, int b) {
            return a + b;
        }
    }

    static ContractId<Calculator> CALCULATOR = ContractId.create(Calculator.class, 0, METHOD_MAPPER_FACTORY);

    static Server SERVER = new Server(
        CALCULATOR.service(new CalculatorImpl())
    );

    public static void main(final String... args) {

        // start server
        new SimpleSocketTransport(EXECUTOR, SERIALIZER, SERVER).start(EXECUTOR, SocketBinder.create(ADDRESS));

        // use client
        Client client = SimpleSocketTransport.client(SERIALIZER, SocketConnector.create(ADDRESS));
        Calculator calculator = client.proxy(CALCULATOR);
        System.out.println("2 + 3 = " + calculator.add(2, 3));

    }

}
