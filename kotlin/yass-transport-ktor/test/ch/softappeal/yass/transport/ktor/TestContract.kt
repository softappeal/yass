package ch.softappeal.yass.transport.ktor

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.fast.*
import ch.softappeal.yass.transport.*
import kotlin.system.*
import kotlin.test.*

interface TestService {
    suspend fun divide(a: Int, b: Int): Int
    suspend fun echo(value: Any?): Any?
    @OneWay
    suspend fun oneWay()

    suspend fun noResult()
}

class DivisionByZeroException : RuntimeException()

val TestServiceImpl = object : TestService {
    override suspend fun divide(a: Int, b: Int): Int {
        if (b == 0) throw DivisionByZeroException()
        return a / b
    }

    override suspend fun echo(value: Any?): Any? {
        return value
    }

    override suspend fun oneWay() {}
    override suspend fun noResult() {}
}

enum class Color { Red, Green, Blue }

private val ContractSerializer = sSimpleFastSerializer(
    listOf(
        SIntSerializer,
        SStringSerializer
    ),
    listOf(
        DivisionByZeroException::class,
        Color::class
    ),
    skipping = false
)

val MessageSerializer = sMessageSerializer(ContractSerializer)

val TestServiceId = contractId<TestService>(1, SimpleMethodMapperFactory)

private fun printer(side: String): SInterceptor = { method, arguments, invocation ->
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

val ClientPrinter = printer("client")
val ServerPrinter = printer("server")

suspend fun TestService.test() {
    assertEquals(4, divide(12, 3))
    sAssertFailsWith<DivisionByZeroException> { divide(12, 0) }
    assertNull(echo(null))
    oneWay()
    noResult()
    assertEquals(Color.Red, echo(Color.Red))
}

suspend fun TestService.performance(times: Int) {
    repeat(2) {
        println(measureTimeMillis {
            repeat(times) {
                assertEquals(4, divide(12, 3))
            }
        })
    }
}
