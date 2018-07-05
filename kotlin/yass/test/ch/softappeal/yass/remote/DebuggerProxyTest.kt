package ch.softappeal.yass.remote

import ch.softappeal.yass.proxy
import org.junit.Test

inline fun <reified C : Any> debuggerProxy(implementation: C): C = proxy(C::class.java, implementation, { method, _, invocation ->
    if ("toString" == method.name && method.parameterCount == 0) "<Proxy>" else invocation()
})

private fun test(calculator: Calculator) {
    println("${calculator.divide(12, 3)}")
    println("$calculator")
}

class DebuggerProxyTest {
    @Test
    fun proxy() {
        test(proxy(CalculatorImpl))
    }

    @Test
    fun debuggerProxy() {
        test(debuggerProxy(CalculatorImpl))
    }
}
