@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

typealias Invocation = () -> Any?

/** Should return invocation(). */
typealias Interceptor = (method: Method, arguments: List<Any?>, invocation: Invocation) -> Any?

val DirectInterceptor: Interceptor = { _, _, invocation -> invocation() }

fun compositeInterceptor(first: Interceptor, second: Interceptor): Interceptor = when {
    first === DirectInterceptor -> second
    second === DirectInterceptor -> first
    else -> { method, arguments, invocation -> first(method, arguments) { second(method, arguments, invocation) } }
}

@SafeVarargs
fun compositeInterceptor(vararg interceptors: Interceptor): Interceptor {
    var c = DirectInterceptor
    for (interceptor in interceptors) c = compositeInterceptor(c, interceptor)
    return c
}

internal fun invoke(method: Method, implementation: Any, arguments: List<Any?>): Any? = try {
    method.invoke(implementation, *arguments.toTypedArray())
} catch (e: InvocationTargetException) {
    throw e.cause!!
}

internal fun invoke(interceptor: Interceptor, method: Method, implementation: Any, arguments: List<Any?>): Any? =
    interceptor(method, arguments) { invoke(method, implementation, arguments) }

internal fun args(arguments: Array<Any?>?): List<Any?> =
    if (arguments == null) emptyList() else listOf(*arguments)

internal fun <C : Any> proxy(contract: Class<C>, invocationHandler: InvocationHandler): C =
    contract.cast(Proxy.newProxyInstance(contract.classLoader, arrayOf(contract), invocationHandler))

@PublishedApi
@SafeVarargs
internal fun <C : Any> proxy(contract: Class<C>, implementation: C, vararg interceptors: Interceptor): C {
    val interceptor = compositeInterceptor(*interceptors)
    if (interceptor === DirectInterceptor) return implementation
    return proxy(
        contract,
        InvocationHandler { _, method, arguments -> invoke(interceptor, method, implementation, args(arguments)) }
    )
}

@SafeVarargs
inline fun <reified C : Any> proxy(implementation: C, vararg interceptors: Interceptor): C =
    proxy(C::class.java, implementation, *interceptors)

fun <T> threadLocalInterceptor(threadLocal: ThreadLocal<T>, value: T?): Interceptor =
    { _, _, invocation ->
        val oldValue = threadLocal.get()
        threadLocal.set(value)
        try {
            invocation()
        } finally {
            threadLocal.set(oldValue)
        }
    }
