package ch.softappeal.yass

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.fail

private val METHOD = Any::class.java.getMethod("toString")
private val ARGUMENTS = listOf(0)

interface Calculator {
    fun one(): Int
    fun minus(a: Int): Int
    fun divide(a: Int, b: Int): Int
    fun echo(a: String?): String?
}

val CalculatorImpl = object : Calculator {
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
    print("$method( $arguments )")
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
            DirectInterceptor(METHOD, ARGUMENTS) { result },
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
            assertSame(METHOD, method)
            assertSame(ARGUMENTS, arguments)
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
        assertEquals((2 * interceptors.size) + offset + 1, interceptor(METHOD, ARGUMENTS, invocation))
        assertEquals((2 * interceptors.size) + 1, step)
    }

    @Test
    fun proxy() {
        assertSame(CalculatorImpl, proxy<Calculator>(CalculatorImpl))
        val calculator = proxy<Calculator>(CalculatorImpl, Printer)
        assertEquals(2, calculator.divide(6, 3))
        try {
            assertEquals(2, calculator.divide(6, 0))
            fail()
        } catch (e: ArithmeticException) {
            println(e)
        }
        assertEquals(1, calculator.one())
        assertEquals(-2, calculator.minus(2))
        assertEquals("echo", calculator.echo("echo"))
        assertNull(calculator.echo(null))
    }

    @Test
    fun javaProxy() {
        assertSame(JavaCalculatorImpl, proxy<JavaCalculator>(JavaCalculatorImpl))
        val calculator = proxy<JavaCalculator>(JavaCalculatorImpl, Printer)
        assertEquals(2, calculator.divide(6, 3))
        try {
            assertEquals(2, calculator.divide(6, 0))
            fail()
        } catch (e: ArithmeticException) {
            println(e)
        }
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
            threadLocalInterceptor(threadLocal, value)(METHOD, ARGUMENTS) {
                assertSame(value, threadLocal.get())
                result
            }
        )
        assertNull(threadLocal.get())
        val oldValue = "oldValue"
        threadLocal.set(oldValue)
        assertSame(
            result,
            threadLocalInterceptor(threadLocal, value)(METHOD, ARGUMENTS) {
                assertSame(value, threadLocal.get())
                result
            }
        )
        assertSame(oldValue, threadLocal.get())
        threadLocalInterceptor(threadLocal, null)(METHOD, ARGUMENTS) {}
    }

}
