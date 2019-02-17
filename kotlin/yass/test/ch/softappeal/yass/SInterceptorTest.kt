package ch.softappeal.yass

import ch.softappeal.yass.remote.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.system.*
import kotlin.test.*

private fun log(location: Int) = println("$location: ${Thread.currentThread().name}")

private val Printer: SInterceptor = { method, arguments, invocation ->
    log(2)
    print("${method.name}$arguments ${coroutineContext.hashCode()}")
    try {
        val result = invocation()
        println(" = $result")
        result
    } catch (e: Exception) {
        println(" threw $e")
        throw e
    }
}

interface OtherStuff {
    suspend fun echo(value: Any?): Any?
    suspend fun noResult()
    @OneWay
    suspend fun oneWay()

    @OneWay
    suspend fun oneWayException()
}

interface Divider : OtherStuff {
    suspend fun divide(a: Int, b: Int): Int
}

val DividerImpl = object : Divider {
    override suspend fun divide(a: Int, b: Int): Int = a / b
    override suspend fun noResult() {}
    override suspend fun echo(value: Any?): Any? {
        delay(100)
        return value
    }

    override suspend fun oneWay() {}
    override suspend fun oneWayException(): Unit = throw RuntimeException("OneWay")
}

interface NoSuspend {
    fun noSuspend()
}

private class SInterceptorTest {
    @Test
    fun direct() = runBlocking {
        val result = Any()
        assertSame(SDirectInterceptor(Method, Arguments) { result }, result)
    }

    @Test
    fun composite2() {
        val interceptor: SInterceptor = { _, _, invocation -> invocation() }
        assertSame(interceptor, sCompositeInterceptor(interceptor, SDirectInterceptor))
        assertSame(interceptor, sCompositeInterceptor(SDirectInterceptor, interceptor))
    }

    @Test
    fun compositeN() = runBlocking {
        val offset = 100
        var step = 0
        fun stepInterceptor(begin: Int, end: Int): SInterceptor = { method, arguments, invocation ->
            println("enter $begin")
            assertSame(Method, method)
            assertSame(Arguments, arguments)
            assertEquals(begin, step)
            step++
            val result = invocation()
            println("exit $end $result")
            assertEquals(end, step)
            assertEquals(result, step + offset)
            step++
            step + offset
        }

        val interceptors = arrayOf(
            stepInterceptor(0, 6),
            stepInterceptor(1, 5),
            stepInterceptor(2, 4)
        )
        val interceptor = sCompositeInterceptor(*interceptors)
        val invocation: SInvocation = {
            assertEquals(step, interceptors.size)
            step++
            step + offset
        }
        assertEquals(
            (2 * interceptors.size) + offset + 1,
            interceptor(Method, Arguments, invocation)
        )
        assertEquals((2 * interceptors.size) + 1, step)
    }

    @Test
    fun proxy() = runBlocking {
        assertSame(DividerImpl, sProxy(DividerImpl, SDirectInterceptor))
        val divider: Divider = sProxy(DividerImpl, Printer)
        println(coroutineContext.hashCode())
        log(1)
        assertEquals(4, divider.divide(12, 3))
        sAssertFailsWith<ArithmeticException> { divider.divide(12, 0) }
        assertNull(divider.echo(null))
        assertEquals("hello", divider.echo("hello"))
        divider.noResult()
        log(3)
        println(coroutineContext.hashCode())
    }

    @Test
    fun noSuspend() {
        assertEquals(
            "'public abstract void ch.softappeal.yass.NoSuspend.noSuspend()' is not a suspend function",
            assertFailsWith<IllegalArgumentException> {
                sProxy(NoSuspend::class, SDirectInterceptor) { _, _ -> }
            }.message
        )
    }

    @Test
    fun performance() = runBlocking {
        var counter = 0
        val divider: Divider = sProxy(DividerImpl, { _, _, invocation ->
            counter++
            invocation()
        })
        val times = 1_000
        repeat(2) {
            println(measureTimeMillis {
                repeat(times) {
                    assertEquals(4, divider.divide(12, 3))
                }
            })
        }
        assertEquals(2 * times, counter)
    }
}
