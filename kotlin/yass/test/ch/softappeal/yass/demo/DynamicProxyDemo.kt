package ch.softappeal.yass.demo

import kotlinx.coroutines.*
import java.lang.reflect.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

interface SFunction {
    suspend fun invoke(): Any?
}

val SRemover: Method = SFunction::class.java.methods[0]

typealias SInvoker = suspend (method: Method, args: List<Any?>) -> Any?

fun Method.sInvoke(args: List<Any?>, cont: Continuation<*>, invoker: SInvoker): Any? =
    SRemover.invoke(object : SFunction {
        override suspend fun invoke(): Any? = invoker(this@sInvoke, args)
    }, cont)

suspend fun Method.sInvoke(implementation: Any, args: List<Any?>): Any? =
    suspendCoroutineUninterceptedOrReturn { cont ->
        sInvoke(args, cont) { _, _ -> invoke(implementation, *args.toTypedArray(), cont) }
    }

typealias SInvocation = suspend () -> Any?
typealias SInterceptor = suspend (method: Method, args: List<Any?>, invocation: SInvocation) -> Any?

@Suppress("UNCHECKED_CAST")
fun <C : Any> sProxy(sContract: Class<C>, implementation: C, interceptor: SInterceptor): C =
    Proxy.newProxyInstance(sContract.classLoader, arrayOf(sContract)) { _, method, args ->
        method.sInvoke(args.take(args.size - 1), args.last() as Continuation<*>) { _, params ->
            interceptor(method, params) { method.sInvoke(implementation, params) }
        }
    } as C

interface Calculator {
    suspend fun add(a: Int, b: Int): Int
}

class CalculatorImpl : Calculator {
    override suspend fun add(a: Int, b: Int) = a + b
}

val Printer: SInterceptor = { method, _, invocation ->
    println(method.name)
    invocation()
}

fun main() = runBlocking {
    val calculator = sProxy(Calculator::class.java, CalculatorImpl(), Printer)
    println(calculator.add(1, 2))
}
