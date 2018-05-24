@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.remote

import ch.softappeal.yass.Interceptor
import ch.softappeal.yass.args
import ch.softappeal.yass.compositeInterceptor
import ch.softappeal.yass.proxy
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CountDownLatch

typealias Tunnel = (request: Request) -> Unit

abstract class ClientInvocation internal constructor(
    methodMapping: MethodMapping, arguments: List<Any?>
) : AbstractInvocation(methodMapping, arguments) {
    abstract fun invoke(asyncSupported: Boolean, tunnel: Tunnel)
    abstract fun settle(reply: Reply)
}

abstract class Client {
    val async = AsyncProxy(this)
    @Throws(Exception::class)
    protected abstract fun invoke(invocation: ClientInvocation)

    internal fun invoke2(invocation: ClientInvocation) = invoke(invocation)
    protected open fun syncInvoke(
        contractId: ContractId<*>, interceptor: Interceptor, method: Method, arguments: List<Any?>
    ): Any? = interceptor(method, arguments) {
        val methodMapping = contractId.methodMapper.map(method)
        val ready = if (methodMapping.oneWay) null else CountDownLatch(1)
        var r: Reply? = null
        invoke(object : ClientInvocation(methodMapping, arguments) {
            override fun invoke(asyncSupported: Boolean, tunnel: Tunnel) = tunnel(Request(contractId.id, methodMapping.id, arguments))
            override fun settle(reply: Reply) {
                if (ready == null) return
                r = reply
                ready.countDown()
            }
        })
        ready?.await()
        r?.process()
    }

    @SafeVarargs
    fun <C : Any> proxy(contractId: ContractId<C>, vararg interceptors: Interceptor): C {
        val interceptor = compositeInterceptor(*interceptors)
        return proxy(
            contractId.contract,
            InvocationHandler { _, method, arguments -> syncInvoke(contractId, interceptor, method, args(arguments)) }
        )
    }
}

private val Promise = ThreadLocal<CompletableFuture<Any>>()

fun <T : Any?> promise(execute: () -> T): CompletionStage<T> {
    val oldPromise = Promise.get()
    val promise = CompletableFuture<T>()
    @Suppress("UNCHECKED_CAST") Promise.set(promise as CompletableFuture<Any>)
    try {
        execute()
    } finally {
        Promise.set(oldPromise)
    }
    return promise
}

class AsyncProxy internal constructor(private val client: Client) {
    fun <C : Any> proxy(contractId: ContractId<C>, interceptor: AsyncInterceptor = DirectAsyncInterceptor): C =
        proxy(contractId.contract, InvocationHandler { _, method, arguments ->
            val methodMapping = contractId.methodMapper.map(method)
            val promise = Promise.get()
            check((promise != null) || methodMapping.oneWay) { "asynchronous request/reply proxy call must be enclosed with 'promise' function" }
            check((promise == null) || !methodMapping.oneWay) { "asynchronous OneWay proxy call must not be enclosed with 'promise' function" }
            client.invoke2(object : ClientInvocation(methodMapping, args(arguments)) {
                override fun invoke(asyncSupported: Boolean, tunnel: Tunnel) {
                    check(asyncSupported) { "asynchronous services not supported (service id ${contractId.id})" }
                    interceptor.entry(this)
                    tunnel(Request(contractId.id, methodMapping.id, args(arguments)))
                }

                override fun settle(reply: Reply) {
                    if (promise == null) return // OneWay
                    try {
                        val result = reply.process()
                        interceptor.exit(this, result)
                        promise.complete(result)
                    } catch (e: Exception) {
                        interceptor.exception(this, e)
                        promise.completeExceptionally(e)
                    }
                }
            })
            handlePrimitiveTypes(method.returnType)
        })
}

private fun handlePrimitiveTypes(type: Class<*>): Any? = when (type) {
    Boolean::class.javaPrimitiveType -> java.lang.Boolean.FALSE
    Byte::class.javaPrimitiveType -> 0.toByte()
    Short::class.javaPrimitiveType -> 0.toShort()
    Int::class.javaPrimitiveType -> 0
    Long::class.javaPrimitiveType -> 0.toLong()
    Char::class.javaPrimitiveType -> 0.toChar()
    Float::class.javaPrimitiveType -> 0.toFloat()
    Double::class.javaPrimitiveType -> 0.toDouble()
    else -> null
}
