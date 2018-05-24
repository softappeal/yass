# yass (Yet Another Service Solution)

* is a small library for efficient peer-to-peer communication
  * Kotlin
  * TypeScript
  * Python 2 & 3 (with support for type hints)
  * high throughput, low latency, reactive services

* supports type-safe contracts with DTOs and interfaces

* supports request/reply and OneWay style method invocations

* supports sync/async client/server invocations

* supports interceptors

* provides session based bidirectional messaging

* provides transports for
  * socket (including TLS)
  * WebSocket

* has a fast and compact binary serializer

* needs no third-party libraries

* uses https://semver.org

* is Open Source (BSD-3-Clause license)
  * Kotlin artifacts on https://search.maven.org (GroupId: ch.softappeal.yass)

## HelloWorld

```kotlin
interface Calculator {
    fun add(a: Int, b: Int): Int
}

class CalculatorImpl : Calculator {
    override fun add(a: Int, b: Int) = a + b
}

fun useCalculator(calculator: Calculator) {
    println("2 + 3 = " + calculator.add(2, 3))
}

fun main(args: Array<String>) {
    val calculatorId = contractId<Calculator>(0, SimpleMethodMapperFactory)
    val messageSerializer = messageSerializer(JavaSerializer)
    val address = InetSocketAddress("localhost", 28947)
    val executor = Executors.newCachedThreadPool()
    val server = Server(service(calculatorId, CalculatorImpl()))
    socketServer(ServerSetup(server, messageSerializer), executor)
        .start(executor, socketBinder(address))
    val client = socketClient(ClientSetup(messageSerializer), socketConnector(address))
    useCalculator(client.proxy(calculatorId))
}
```
