# yass (Yet Another Service Solution)

* is a small library for efficient peer-to-peer communication
    * Kotlin/Java
    * TypeScript
    * Python 3 (with support for type hints)
    * high throughput, low latency, reactive services
* supports type-safe contracts with DTOs and interfaces
* supports request/reply and OneWay style method invocations
* supports sync/async client/server invocations
* supports Kotlin coroutines (suspend functions)
* supports interceptors
* provides session based bidirectional messaging
* provides transports for
    * socket (including TLS)
    * WebSocket
* has a fast and compact binary serializer that can skip unknown/new fields
* uses https://semver.org
* is Open Source (BSD-3-Clause license)
    * Kotlin artifacts on https://search.maven.org (GroupId: ch.softappeal.yass)

## HelloWorld

```kotlin
interface Calculator {
    suspend fun add(a: Int, b: Int): Int
}

class CalculatorImpl : Calculator {
    override suspend fun add(a: Int, b: Int) = a + b
}

suspend fun useCalculator(calculator: Calculator) {
    println("2 + 3 = " + calculator.add(2, 3))
}

val CalculatorId = contractId<Calculator>(0, SimpleMethodMapperFactory)

val Server = SServer(
    SService(CalculatorId, CalculatorImpl())
)

val ContractSerializer = sSimpleFastSerializer(listOf(SIntSerializer), listOf())

val MessageSerializer = sMessageSerializer(ContractSerializer)

fun main() {
    val tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
    val address = InetSocketAddress("localhost", 28947)
    runBlocking {
        val serverJob = sStartSocketServer(this, tcp.bind(address), SServerSetup(Server, MessageSerializer))
        val client = sSocketClient(SClientSetup(MessageSerializer)) { tcp.connect(address) }
        useCalculator(client.proxy(CalculatorId))
        serverJob.cancel()
    }
}
```
