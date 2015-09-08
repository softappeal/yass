# yass (Yet Another Service Solution)

* is a small library for efficient peer-to-peer communication
  * Java
  * TypeScript/JavaScript
  * high throughput, low latency

* supports type-safe contracts with DTOs and interfaces

* supports request/reply and oneway style method invocations

* supports interceptors

* provides session based bidirectional messaging

* provides transports for
  * socket (including TLS)
  * WebSocket

* has a fast and compact binary serializer

* needs no third-party libraries

* uses http://semver.org

* is Open Source (BSD-style license)
  * repository: https://github.com/softappeal/yass
  * wiki: https://github.com/softappeal/yass/wiki
  * artifacts on MavenCentral: http://search.maven.org
    * groupId="ch.softappeal.yass"
    * artifactId="yass"
  * tutorial: see https://github.com/softappeal/yass/tree/master/src/tutorial

## HelloWorld

```java
public final class HelloWorld {

    public interface Calculator {
        int add(int a, int b);
    }

    public static final class CalculatorImpl implements Calculator {
        @Override public int add(final int a, final int b) {
            return (a + b);
        }
    }

    public static final ContractId<Calculator> CALCULATOR_ID = ContractId.create(Calculator.class, 0);

    public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);

    public static void main(final String... args) throws InterruptedException {
        final ExecutorService executor = SocketBuilder.newExecutorService();

        // start server
        new SocketBuilder(executor)
            .addService(CALCULATOR_ID, new CalculatorImpl())
            .start(ADDRESS);

        // connect client
        new SocketBuilder(executor)
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
```

see https://github.com/softappeal/yass/tree/master/src/tutorial/java/ch/softappeal/yass/tutorial
