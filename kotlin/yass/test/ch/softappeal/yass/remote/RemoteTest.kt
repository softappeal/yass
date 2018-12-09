package ch.softappeal.yass.remote

import ch.softappeal.yass.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.*
import java.util.concurrent.*
import kotlin.system.*
import kotlin.test.*

interface Calculator {
    @OneWay
    fun oneWay()

    fun twoWay()
    fun divide(a: Int, b: Int): Int
    fun echo(value: String?): String?
}

private interface AsyncCalculator {
    fun oneWay()
    fun twoWay(): CompletionStage<Unit>
    fun divide(a: Int, b: Int): CompletionStage<Int>
    fun echo(value: String?): CompletionStage<String?>
}

private interface SuspendCalculator {
    fun oneWay()
    suspend fun twoWay()
    suspend fun divide(a: Int, b: Int): Int
    suspend fun echo(value: String?): String?
}

private fun asyncCalculator(asyncCalculator: Calculator): AsyncCalculator = object : AsyncCalculator {
    override fun oneWay() {
        asyncCalculator.oneWay()
    }

    override fun twoWay(): CompletionStage<Unit> {
        return promise { asyncCalculator.twoWay() }
    }

    override fun divide(a: Int, b: Int): CompletionStage<Int> {
        return promise { asyncCalculator.divide(a, b) }
    }

    override fun echo(value: String?): CompletionStage<String?> {
        return promise { asyncCalculator.echo(value) }
    }
}

private fun suspendCalculator(asyncCalculator: Calculator): SuspendCalculator = object : SuspendCalculator {
    private suspend fun <T> coroutine(execute: () -> T): T = promise(execute).await()

    override fun oneWay() {
        asyncCalculator.oneWay()
    }

    override suspend fun twoWay() {
        return coroutine { asyncCalculator.twoWay() }
    }

    override suspend fun divide(a: Int, b: Int): Int {
        return coroutine { asyncCalculator.divide(a, b) }
    }

    override suspend fun echo(value: String?): String? {
        return coroutine { asyncCalculator.echo(value) }
    }
}

val CalculatorImpl = object : Calculator {
    override fun oneWay() {
        println("oneWay")
    }

    override fun twoWay() {
        println("twoWay")
    }

    override fun divide(a: Int, b: Int) = a / b
    override fun echo(value: String?) = value
}

val AsyncCalculatorImpl = object : Calculator {
    private fun sleep(execute: (completer: Completer) -> Unit) {
        val completer = completer
        Thread {
            TimeUnit.MILLISECONDS.sleep(100L)
            execute(completer)
        }.start()
    }

    override fun oneWay() {
        println("oneWay")
    }

    override fun twoWay() {
        println("twoWay")
        sleep(Completer::complete)
    }

    override fun divide(a: Int, b: Int): Int {
        sleep {
            if (b == 0)
                it.completeExceptionally(ArithmeticException("/ by zero"))
            else
                it.complete(a / b)
        }
        return 0
    }

    override fun echo(value: String?): String? {
        sleep { completer -> completer.complete(value) }
        return null
    }
}

private fun testObjectMethods(calculator: Calculator) {
    assertTrue(calculator.equals(calculator))
    assertFalse(calculator.equals(""))
    assertEquals(Calculator::class.java.hashCode(), calculator.hashCode())
    assertTrue(
        "<yass proxy for ContractId(ch.softappeal.yass.remote.Calculator, 123)>" == calculator.toString() ||
            "<yass proxy for ContractId(ch.softappeal.yass.remote.Calculator, 321)>" == calculator.toString()
    )
}

fun useSyncClient(calculator: Calculator) {
    testObjectMethods(calculator)
    assertEquals(4, calculator.divide(12, 3))
    assertEquals(
        "/ by zero",
        assertFailsWith<ArithmeticException> { calculator.divide(12, 0) }.message
    )
    calculator.twoWay()
    assertNull(calculator.echo(null))
    assertEquals("hello", calculator.echo("hello"))
    calculator.oneWay()
}

private fun useAsyncClient(asyncCalculator: Calculator) {
    assertEquals(
        "asynchronous OneWay proxy call must not be enclosed with 'promise' function",
        assertFailsWith<IllegalStateException> { promise { asyncCalculator.oneWay() } }.message
    )
    assertEquals(
        "asynchronous request/reply proxy call must be enclosed with 'promise' function",
        assertFailsWith<IllegalStateException> { asyncCalculator.twoWay() }.message
    )
    testObjectMethods(asyncCalculator)
    val calculator = suspendCalculator(asyncCalculator)
    runBlocking {
        assertEquals(4, calculator.divide(12, 3))
        try {
            calculator.divide(12, 0)
            fail()
        } catch (e: ArithmeticException) {
            assertEquals("/ by zero", e.message)
        }
        assertNull(calculator.twoWay())
        assertNull(calculator.echo(null))
        assertEquals("hello", calculator.echo("hello"))
        calculator.oneWay()
    }
}

fun useClient(calculator: Calculator, asyncCalculator: Calculator) {
    useSyncClient(calculator)
    useAsyncClient(asyncCalculator)
}

val calculatorId = contractId<Calculator>(123, SimpleMethodMapperFactory)
val asyncCalculatorId = contractId<Calculator>(321, SimpleMethodMapperFactory)

private fun printer(side: String): Interceptor {
    return { method, arguments, invocation ->
        fun print(type: String, data: Any?, arguments: List<Any?>? = null) =
            println("$side - ${Thread.currentThread().name} - $type - $data ${arguments ?: ""}")
        print("enter", method.name, arguments)
        try {
            val result = invocation()
            print("exit", result)
            result
        } catch (e: Exception) {
            print("exception", e)
            throw e
        }
    }
}

val clientPrinter = printer("client")
val serverPrinter = printer("server")

private var counter = 0

fun asyncPrinter(side: String) = object : AsyncInterceptor {
    override fun entry(invocation: AbstractInvocation) {
        invocation.context = counter++
        println("$side - ${invocation.context} - enter ${invocation.methodMapping.method.name}${invocation.arguments}")
    }

    override fun exit(invocation: AbstractInvocation, result: Any?) {
        println("$side - ${invocation.context} - exit $result")
    }

    override fun exception(invocation: AbstractInvocation, exception: Exception) {
        println("$side - ${invocation.context} - exception $exception")
    }
}

fun performance(client: Client) {
    val calculator = client.proxy(calculatorId)
    val iterations = 1 // 10_000
    for (warmUp in 1..2) {
        println("iterations = $iterations, one took ${1_000.0 * measureTimeMillis {
            for (i in 1..iterations)
                assertEquals(4, calculator.divide(12, 3))
        } / iterations}us")
    }
}

private fun client(server: Server, clientAsyncSupported: Boolean) = object : Client() {
    override fun invoke(invocation: ClientInvocation) = invocation.invoke(clientAsyncSupported) { request ->
        Thread {
            server.invocation(true, request)
                .invoke({ println("cleanup ${invocation.methodMapping.method.name}") }) { reply ->
                    invocation.settle(reply)
                }
        }.start()
    }
}

class RemoteTest {
    @Test
    fun invocations() {
        val client = client(
            Server(
                Service(calculatorId, CalculatorImpl, serverPrinter),
                AsyncService(asyncCalculatorId, AsyncCalculatorImpl, asyncPrinter("server"))
            ), true
        )
        useClient(
            client.proxy(calculatorId, clientPrinter),
            client.asyncProxy(asyncCalculatorId, asyncPrinter("client"))
        )
    }

    @Test
    fun asyncProxy() {
        val client = client(Server(Service(calculatorId, CalculatorImpl)), false)
        client.proxy(calculatorId).twoWay()
        assertEquals(
            "asynchronous services not supported (service id 123)",
            assertFailsWith<IllegalStateException> { promise { client.asyncProxy(calculatorId).twoWay() } }.message
        )
    }

    @Test
    fun services() {
        val service = Service(calculatorId, CalculatorImpl)
        assertEquals(
            "service id 123 already added",
            assertFailsWith<IllegalStateException> { Server(service, service) }.message
        )
        assertEquals(
            "no service id 987 found (method id 0)",
            assertFailsWith<IllegalStateException> {
                Server(service).invocation(true, Request(987, 0, listOf()))
            }.message
        )
    }

    @Test
    fun asyncService() {
        val asyncService = AsyncService(calculatorId, CalculatorImpl)
        Server(asyncService).invocation(true, Request(calculatorId.id, 0, listOf()))
        assertEquals(
            "asynchronous services not supported (service id 123)",
            assertFailsWith<IllegalStateException> {
                Server(asyncService).invocation(false, Request(calculatorId.id, 0, listOf()))
            }.message
        )
    }

    @Test
    fun noCompleter() = assertEquals(
        "no active asynchronous request/reply service invocation",
        assertFailsWith<IllegalStateException> { completer }.message
    )

    @Test
    fun performance() {
        fun client(server: Server) = object : Client() {
            override fun invoke(invocation: ClientInvocation) = invocation.invoke(false) { request ->
                server.invocation(true, request).invoke { reply -> invocation.settle(reply) }
            }
        }
        performance(client(Server(Service(calculatorId, CalculatorImpl))))
    }
}
