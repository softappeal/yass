[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ch.softappeal.yass/yass/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ch.softappeal.yass/yass)
[![Build Status](https://travis-ci.org/softappeal/yass.svg?branch=master)](https://travis-ci.org/softappeal/yass)

# yass (Yet Another Service Solution)

* is a small library for efficient peer-to-peer communication
  * Java
  * TypeScript/JavaScript
  * Python 2 & 3 (with support for type hints)
  * high throughput, low latency, reactive services

* supports type-safe contracts with DTOs and interfaces

* supports request/reply and oneWay style method invocations

* supports sync/async client/server invocations

* supports interceptors

* provides session based bidirectional messaging

* provides transports for
  * socket (including TLS)
  * WebSocket

* has a fast and compact binary serializer

* needs no third-party libraries

* uses http://semver.org

* is Open Source (BSD-3-Clause license)

## HelloWorld

```java
public class HelloWorld {

    public interface Calculator {
        int add(int a, int b);
    }

    static class CalculatorImpl implements Calculator {
        @Override public int add(int a, int b) {
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
```
