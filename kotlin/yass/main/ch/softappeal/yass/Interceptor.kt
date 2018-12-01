package ch.softappeal.yass

import java.lang.reflect.*

@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class OnlyNeededForJava

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
    var composite = DirectInterceptor
    for (interceptor in interceptors) composite = compositeInterceptor(composite, interceptor)
    return composite
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

@Suppress("UNCHECKED_CAST")
internal fun <C : Any> proxy(contract: Class<C>, invocationHandler: InvocationHandler): C =
    Proxy.newProxyInstance(contract.classLoader, arrayOf(contract), invocationHandler) as C

@OnlyNeededForJava
@SafeVarargs
fun <C : Any> proxy(contract: Class<C>, implementation: C, vararg interceptors: Interceptor): C {
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

fun <T : Any?> threadLocalInterceptor(threadLocal: ThreadLocal<T>, value: T): Interceptor = { _, _, invocation ->
    val oldValue = threadLocal.get()
    threadLocal.set(value)
    try {
        invocation()
    } finally {
        threadLocal.set(oldValue)
    }
}
