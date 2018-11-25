package ch.softappeal.yass.remote

import ch.softappeal.yass.Interceptor
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

interface Calculator {
    @OneWay
    fun oneWay()

    fun twoWay()
    fun divide(a: Int, b: Int): Int
    fun echo(value: String?): String?
}

val CalculatorImpl = object : Calculator {
    override fun oneWay() {}
    override fun twoWay() {}
    override fun divide(a: Int, b: Int) = a / b
    override fun echo(value: String?) = value
}

val calculatorId = contractId<Calculator>(123, SimpleMethodMapperFactory)

fun useClient(calculator: Calculator) {
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

private fun printer(client: Boolean): Interceptor {
    return { method, arguments, invocation ->
        fun print(type: String, data: Any?, arguments: List<Any?>? = null) =
            println(
                "${if (client) "client" else "server"} - ${Thread.currentThread().name} -" +
                        " $type - $data ${arguments ?: ""}"
            )
        print("enter", method.name, arguments)
        try {
            val result = invocation()
            print("exit", result)
            result
        } catch (e: Exception) {
            print("exception", e)
            throw e
        } finally {
            if (client) println()
        }
    }
}

val clientPrinter = printer(true)
val serverPrinter = printer(false)

private fun sleep(execute: (completer: Completer) -> Unit) {
    val completer = completer
    Thread {
        TimeUnit.MILLISECONDS.sleep(100L)
        execute(completer)
    }.start()
}

private val AsyncCalculatorImpl = object : Calculator {
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

private val service = Service(calculatorId, CalculatorImpl)
private val asyncService = AsyncService(calculatorId, CalculatorImpl)

private fun client(server: Server, clientAsyncSupported: Boolean) = object : Client() {
    override fun syncInvoke(
        contractId: ContractId<*>,
        interceptor: Interceptor,
        method: Method,
        arguments: List<Any?>
    ): Any? {
        println("sync ${method.name}")
        return super.syncInvoke(contractId, interceptor, method, arguments)
    }

    override fun invoke(invocation: ClientInvocation) = invocation.invoke(clientAsyncSupported) { request ->
        Thread {
            server.invocation(true, request).invoke { reply -> invocation.settle(reply) }
        }.start()
    }
}

private var counter = 0

private fun asyncPrinter(side: String) = object : AsyncInterceptor {
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

private fun testObjectMethods(calculator: Calculator) {
    assertTrue(calculator.equals(calculator))
    assertFalse(calculator.equals(""))
    assertEquals(Calculator::class.java.hashCode(), calculator.hashCode())
    assertEquals("<proxy>", calculator.toString())
}

private fun syncClient(client: Client) {
    val calculator = client.proxy(calculatorId, clientPrinter)
    calculator.oneWay()
    calculator.twoWay()
    assertEquals(4, calculator.divide(12, 3))
    assertEquals(
        "/ by zero",
        assertFailsWith<ArithmeticException> { calculator.divide(12, 0) }.message
    )
    assertEquals("hello", calculator.echo("hello"))
    testObjectMethods(calculator)
}

class RemoteTest {
    @Test
    fun syncClientSyncServer() =
        syncClient(client(Server(Service(calculatorId, CalculatorImpl, serverPrinter)), true))

    @Test
    fun syncClientAsyncServer() = syncClient(
        client(
            Server(AsyncService(calculatorId, AsyncCalculatorImpl, asyncPrinter("server"))),
            true
        )
    )

    @Test
    fun asyncClientAsyncServer() {
        val client = client(
            Server(AsyncService(calculatorId, AsyncCalculatorImpl, asyncPrinter("server"))),
            true
        )
        val calculator = client.asyncProxy(calculatorId, asyncPrinter("client"))

        testObjectMethods(calculator)

        calculator.oneWay()
        assertEquals(
            "asynchronous OneWay proxy call must not be enclosed with 'promise' function",
            assertFailsWith<IllegalStateException> { promise { calculator.oneWay() } }.message
        )
        promise { calculator.twoWay() }.thenAcceptAsync(::println)
        assertEquals(
            "asynchronous request/reply proxy call must be enclosed with 'promise' function",
            assertFailsWith<IllegalStateException> { calculator.twoWay() }.message
        )
        val result = AtomicInteger(0)
        promise { calculator.divide(12, 3) }.thenAcceptAsync { result.set(it) }
        promise { calculator.divide(12, 0) }.whenCompleteAsync { _, e -> println(e) }
        promise { calculator.echo("hello") }.thenAcceptAsync(::println)
        println("done")
        assertEquals(0, result.get())
        TimeUnit.MILLISECONDS.sleep(200L)
        assertEquals(4, result.get())

        println("coroutine")
        suspend fun <T> coroutine(execute: () -> T): T = promise(execute).await()
        runBlocking {
            assertNull(coroutine { calculator.twoWay() })
            assertEquals(4, coroutine { calculator.divide(12, 3) })
            try {
                coroutine { calculator.divide(12, 0) }
                fail()
            } catch (e: ArithmeticException) {
                assertEquals("/ by zero", e.message)
            }
            assertEquals("hello", coroutine { calculator.echo("hello") })
        }
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
    fun duplicatedService() = assertEquals(
        "service id 123 already added",
        assertFailsWith<IllegalStateException> { Server(service, service) }.message
    )

    @Test
    fun noService() = assertEquals(
        "no service id 987 found (method id 0)",
        assertFailsWith<IllegalStateException> {
            Server(service).invocation(true, Request(987, 0, listOf()))
        }.message
    )

    @Test
    fun asyncService() {
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
                server.invocation(false, request).invoke { reply -> invocation.settle(reply) }
            }
        }
        performance(client(Server(Service(calculatorId, CalculatorImpl))))
    }
}
