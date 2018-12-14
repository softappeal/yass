package ch.softappeal.yass

import kotlin.test.*

class JInterceptorTest {
    @Test
    fun test() {
        val interceptor = TestInterceptor()
        val arguments = emptyList<Any>()
        val exception = RuntimeException()
        val result = "result"
        assertSame(result, interceptor.invoke(Method, arguments) { result })
        assertSame(
            exception,
            assertFailsWith<Exception> { interceptor.invoke(Method, emptyList<Any>()) { throw exception } }
        )
    }

    @Test
    fun proxyTest() {
        val calculator = proxy(JavaCalculator::class.java, JavaCalculatorImpl)
        assertEquals(2, calculator.divide(6, 3))
        assertEquals(
            "/ by zero",
            assertFailsWith<ArithmeticException> { calculator.divide(6, 0) }.message
        )
        assertEquals(1, calculator.one())
        assertEquals(-2, calculator.minus(2))
        assertEquals("echo", calculator.echo("echo"))
        assertNull(calculator.echo(null))
    }
}
