# yass (Yet Another Service Solution)

* is a small library for efficient peer-to-peer communication
  * Java
  * TypeScript/JavaScript
  * Python 3 (with support for optional static type checker 'mypy')
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

* is Open Source (BSD-style license)
  * repository: https://github.com/softappeal/yass
  * wiki: https://github.com/softappeal/yass/wiki
  * Java artifacts on MavenCentral: http://search.maven.org
    * groupId="ch.softappeal.yass"
    * artifactId="yass"
  * TypeScript artifacts on https://www.npmjs.com: npm install softappeal-yass

## HelloWorld

```java
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

// start server
new SimpleSocketTransport(EXECUTOR, SERIALIZER, SERVER).start(EXECUTOR, new SimpleSocketBinder(ADDRESS));

// use client
Client client = SimpleSocketTransport.client(SERIALIZER, new SimpleSocketConnector(ADDRESS));
Calculator calculator = client.proxy(CALCULATOR);
System.out.println("2 + 3 = " + calculator.add(2, 3));
```
