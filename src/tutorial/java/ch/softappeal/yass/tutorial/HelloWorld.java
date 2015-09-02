package ch.softappeal.yass.tutorial;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.transport.socket.SocketHelper;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class HelloWorld {

    public interface Calculator {
        int add(int a, int b);
    }

    public static final class CalculatorImpl implements Calculator {
        @Override public int add(final int a, final int b) {
            return (a + b);
        }
    }

    public static final ContractId<Calculator> CALCULATOR_ID = ContractId.create(
        Calculator.class, 0
    );

    public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

    public static void main(final String... args) throws InterruptedException {
        final ExecutorService executor = SocketHelper.newExecutorService();

        // start server
        new SocketHelper(executor)
            .addService(CALCULATOR_ID, new CalculatorImpl())
            .start(ADDRESS);

        // connect client
        new SocketHelper(executor)
            .opened(session -> { // called when session has been opened
                final Calculator calculator = session.proxy(CALCULATOR_ID);
                System.out.println("2 + 3 = " + calculator.add(2, 3));
                session.close();
            })
            .connect(ADDRESS);

        TimeUnit.SECONDS.sleep(1); // give some time to connect
        executor.shutdownNow();
    }

}
