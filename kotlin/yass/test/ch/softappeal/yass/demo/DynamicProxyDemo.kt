package ch.softappeal.yass.demo

import kotlinx.coroutines.*
import java.lang.reflect.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

// Intercepting suspend functions using a Java dynamic proxy:

interface SFunction {
    suspend fun invoke(): Any?
}

val SRemover: Method = SFunction::class.java.methods[0]

typealias SInvoker = suspend (method: Method, args: List<Any?>) -> Any?

fun Method.sInvoke(
    args: List<Any?>, continuation: Continuation<*>, invoker: SInvoker
): Any? = SRemover.invoke(object : SFunction {
    override suspend fun invoke(): Any? = invoker(this@sInvoke, args)
}, continuation)

suspend fun Method.sInvoke(implementation: Any, args: List<Any?>): Any? =
    suspendCoroutineUninterceptedOrReturn { continuation ->
        sInvoke(args, continuation) { _, _ ->
            invoke(implementation, *args.toTypedArray(), continuation)
        }
    }

/** Creates a proxy for [sContract] that intercepts calls to [implementation]. */
@Suppress("UNCHECKED_CAST")
fun <C : Any> sProxy(
    sContract: Class<C>, implementation: C, interceptor: SInterceptor
): C = Proxy.newProxyInstance(
    sContract.classLoader, arrayOf(sContract)
) { _, method, args ->
    method.sInvoke(
        args.take(args.size - 1), args.last() as Continuation<*>
    ) { _, params ->
        interceptor(method, params) { method.sInvoke(implementation, params) }
    }
} as C

// The following code uses the function [sProxy] to intercept suspend functions.
// It prints:
//   add[1, 2] called
//   3

typealias SInvocation = suspend () -> Any?
typealias SInterceptor =
    suspend (method: Method, args: List<Any?>, invocation: SInvocation) -> Any?

interface Calculator {
    suspend fun add(a: Int, b: Int): Int
}

class CalculatorImpl : Calculator {
    override suspend fun add(a: Int, b: Int) = a + b
}

val Printer: SInterceptor = { method, args, invocation ->
    println("${method.name}$args called")
    invocation()
}

fun main() = runBlocking {
    val calculator = sProxy(Calculator::class.java, CalculatorImpl(), Printer)
    println(calculator.add(1, 2))
}

// So now the questions are:
//  - Is this a good/problematic solution?
//  - Is there a better solution?
