package ch.softappeal.yass

import java.lang.reflect.*
import java.lang.reflect.Proxy.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.reflect.*

typealias SInvocation = suspend () -> Any?

/** Should return invocation(). */
typealias SInterceptor = suspend (method: Method, arguments: List<Any?>, invocation: SInvocation) -> Any?

val SDirectInterceptor: SInterceptor = { _, _, invocation -> invocation() }

fun sCompositeInterceptor(first: SInterceptor, second: SInterceptor): SInterceptor = when {
    first === SDirectInterceptor -> second
    second === SDirectInterceptor -> first
    else -> { method, arguments, invocation ->
        first(method, arguments) { second(method, arguments, invocation) }
    }
}

@SafeVarargs
fun sCompositeInterceptor(vararg interceptors: SInterceptor): SInterceptor {
    var composite = SDirectInterceptor
    for (interceptor in interceptors) composite = sCompositeInterceptor(composite, interceptor)
    return composite
}

internal typealias SInvoker = suspend (method: Method, arguments: List<Any?>) -> Any?

private interface SFunction {
    suspend fun invoke(): Any?
}

private val SRemover = SFunction::class.java.methods[0]

private fun sRemove(
    interceptor: SInterceptor, method: Method, arguments: List<Any?>, continuation: Continuation<*>, invoker: SInvoker
): Any? = try {
    SRemover.invoke(object : SFunction {
        override suspend fun invoke(): Any? =
            interceptor(method, arguments) { invoker(method, arguments) }
    }, continuation)
} catch (e: InvocationTargetException) {
    throw e.cause!!
}

internal suspend fun Method.sInvoke(interceptor: SInterceptor, implementation: Any, arguments: List<Any?>): Any? =
    suspendCoroutineUninterceptedOrReturn { continuation ->
        sRemove(interceptor, this, arguments, continuation) { _, _ ->
            try {
                invoke(implementation, *arguments.toTypedArray(), continuation)
            } catch (e: InvocationTargetException) {
                throw e.cause!!
            }
        }
    }

internal fun Method.isSuspend(): Boolean {
    val lastParameterType = parameterTypes.lastOrNull()
    return lastParameterType != null && Continuation::class.java.isAssignableFrom(lastParameterType)
}

@Suppress("UNCHECKED_CAST")
internal fun <C : Any> sProxy(sContract: KClass<C>, interceptor: SInterceptor, invoker: SInvoker): C {
    sContract.java.methods.forEach { method -> require(method.isSuspend()) { "'$method' is not a suspend function" } }
    return newProxyInstance(sContract.java.classLoader, arrayOf(sContract.java)) { _, method, arguments ->
        val continuation = arguments.last() as Continuation<*>
        val argumentsNoContinuation = arguments.take(arguments.size - 1)
        sRemove(interceptor, method, argumentsNoContinuation, continuation) { _, _ ->
            invoker(method, argumentsNoContinuation)
        }
    } as C
}

@PublishedApi
internal fun <C : Any> sProxy(sContract: KClass<C>, implementation: C, interceptor: SInterceptor): C {
    if (interceptor === SDirectInterceptor) return implementation
    return sProxy(sContract, interceptor) { method, arguments ->
        method.sInvoke(SDirectInterceptor, implementation, arguments)
    }
}

@SafeVarargs
inline fun <reified C : Any> sProxy(implementation: C, vararg interceptors: SInterceptor): C =
    sProxy(C::class, implementation, sCompositeInterceptor(*interceptors))
