package ch.softappeal.yass.tutorial;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.socket.SocketBinder;
import ch.softappeal.yass.transport.socket.SocketConnector;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HelloWorld {

    public interface Calculator {
        int add(int a, int b);
    }

    static class CalculatorImpl implements Calculator {
        public int add(int a, int b) {
            return a + b;
        }
    }

    public static void main(String... args) {

        var address = new InetSocketAddress("localhost", 28947);
        var serializer = JavaSerializer.INSTANCE;
        var calculatorId = ContractId.create(Calculator.class, 0, SimpleMethodMapper.FACTORY);

        // start server
        var server = new Server(calculatorId.service(new CalculatorImpl()));
        var executor = Executors.newCachedThreadPool();
        new SimpleSocketTransport(executor, serializer, server).start(executor, SocketBinder.create(address));

        // use client
        var client = SimpleSocketTransport.client(serializer, SocketConnector.create(address));
        var calculator = client.proxy(calculatorId);
        System.out.println("2 + 3 = " + calculator.add(2, 3));

    }

}
