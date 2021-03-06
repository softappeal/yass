package ch.softappeal.yass

import java.lang.reflect.*
import java.lang.reflect.Proxy.*

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
    else -> { method, arguments, invocation ->
        first(method, arguments) { second(method, arguments, invocation) }
    }
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
    if (arguments == null) listOf() else listOf(*arguments)

internal fun <C : Any> proxy(contract: Class<C>, invocationHandler: InvocationHandler): C {
    var proxy: Any? = null
    val objectMethods: Map<Method, (arguments: Array<Any?>?) -> Any?> = mapOf(
        Object::class.java.getMethod("toString") to { _ -> "<yass proxy for '${contract.canonicalName}'>" },
        Object::class.java.getMethod("hashCode") to { _ -> contract.hashCode() },
        Object::class.java.getMethod("equals", Object::class.java) to { arguments -> proxy === arguments!![0] }
    )
    proxy = newProxyInstance(contract.classLoader, arrayOf(contract), InvocationHandler { p, method, arguments ->
        val objectMethod = objectMethods[method]
        if (objectMethod != null) return@InvocationHandler objectMethod(arguments)
        invocationHandler(p, method, arguments)
    })
    @Suppress("UNCHECKED_CAST") return proxy as C
}

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

fun addSuppressed(e: Exception, block: () -> Unit): Unit = try {
    block()
} catch (e2: Exception) {
    e.addSuppressed(e2)
}

fun <T : Any?, B : Any?> threadLocal(threadLocal: ThreadLocal<T>, value: T, block: () -> B): B {
    val oldValue = threadLocal.get()
    threadLocal.set(value)
    try {
        return block()
    } finally {
        threadLocal.set(oldValue)
    }
}

fun <T : Any?> threadLocalInterceptor(threadLocal: ThreadLocal<T>, value: T): Interceptor = { _, _, invocation ->
    threadLocal(threadLocal, value) { invocation() }
}
