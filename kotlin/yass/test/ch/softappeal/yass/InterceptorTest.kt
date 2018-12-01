package ch.softappeal.yass

import java.lang.reflect.*
import kotlin.test.*

val Method: Method = Any::class.java.getMethod("toString")
private val Arguments = listOf(0)

private interface Calculator {
    fun one(): Int
    fun minus(a: Int): Int
    fun divide(a: Int, b: Int): Int
    fun echo(a: String?): String?
}

private class CalculatorImpl : Calculator {
    override fun one() = 1
    override fun minus(a: Int) = -a
    override fun divide(a: Int, b: Int) = a / b
    override fun echo(a: String?) = a
}

val JavaCalculatorImpl = object : JavaCalculator {
    override fun one() = 1
    override fun minus(a: Int) = -a
    override fun divide(a: Int, b: Int) = a / b
    override fun echo(a: String?) = a
}

private val Printer: Interceptor = { method, arguments, invocation ->
    print("$method$arguments")
    try {
        val result = invocation()
        println(" = $result")
        result
    } catch (e: Exception) {
        println(" threw $e")
        throw e
    }
}

class InterceptorTest {
    @Test
    fun direct() {
        val result = Any()
        assertSame(
            DirectInterceptor(Method, Arguments) { result },
            result
        )
    }

    @Test
    fun composite2() {
        val interceptor: Interceptor = { _, _, invocation -> invocation() }
        assertSame(interceptor, compositeInterceptor(interceptor, DirectInterceptor))
        assertSame(interceptor, compositeInterceptor(DirectInterceptor, interceptor))
    }

    @Test
    fun compositeN() {
        val offset = 100
        var step = 0
        fun stepInterceptor(begin: Int, end: Int): Interceptor = { method, arguments, invocation ->
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
        val interceptor = compositeInterceptor(*interceptors)
        val invocation = {
            assertEquals(step, interceptors.size)
            step++
            step + offset
        }
        assertEquals((2 * interceptors.size) + offset + 1, interceptor(Method, Arguments, invocation))
        assertEquals((2 * interceptors.size) + 1, step)
    }

    @Test
    fun proxyTest() {
        val c = CalculatorImpl()
        assertSame(c, proxy(c))
        val calculator: Calculator = proxy<Calculator>(CalculatorImpl(), Printer)
        assertEquals(2, calculator.divide(6, 3))
        assertFailsWith<ArithmeticException> { calculator.divide(6, 0) }
        assertEquals(1, calculator.one())
        assertEquals(-2, calculator.minus(2))
        assertEquals("echo", calculator.echo("echo"))
        assertNull(calculator.echo(null))
    }

    @Test
    fun javaProxyTest() {
        assertSame(JavaCalculatorImpl, proxy(JavaCalculatorImpl))
        val calculator = proxy(JavaCalculatorImpl, Printer)
        assertEquals(2, calculator.divide(6, 3))
        assertFailsWith<ArithmeticException> { calculator.divide(6, 0) }
        assertEquals(1, calculator.one())
        assertEquals(-2, calculator.minus(2))
        assertEquals("echo", calculator.echo("echo"))
        assertNull(calculator.echo(null))
    }

    @Test
    fun threadLocal() {
        val threadLocal = ThreadLocal<String>()
        assertNull(threadLocal.get())
        val result = Any()
        val value = "value"
        assertSame(
            result,
            threadLocalInterceptor(threadLocal, value)(Method, Arguments) {
                assertSame(value, threadLocal.get())
                result
            }
        )
        assertNull(threadLocal.get())
        val oldValue = "oldValue"
        threadLocal.set(oldValue)
        assertSame(
            result,
            threadLocalInterceptor(threadLocal, value)(Method, Arguments) {
                assertSame(value, threadLocal.get())
                result
            }
        )
        assertSame(oldValue, threadLocal.get())
        threadLocalInterceptor(ThreadLocal<String?>(), null)(Method, Arguments) {}
    }
}
