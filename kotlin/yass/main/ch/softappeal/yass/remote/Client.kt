package ch.softappeal.yass.remote

import ch.softappeal.yass.*
import ch.softappeal.yass.proxy
import java.lang.reflect.*
import java.util.concurrent.*

typealias Tunnel = (request: Request) -> Unit

abstract class ClientInvocation internal constructor(
    methodMapping: MethodMapping, arguments: List<Any?>
) : AbstractInvocation(methodMapping, arguments) {
    abstract fun invoke(asyncSupported: Boolean, tunnel: Tunnel)
    abstract fun settle(reply: Reply)
}

private fun <C : Any> proxy(contractId: ContractId<C>, invocation: (method: Method, arguments: List<Any?>) -> Any?): C =
    proxy(contractId.contract, InvocationHandler { _, method, arguments -> invocation(method, args(arguments)) })

abstract class Client {
    @Throws(Exception::class)
    protected abstract fun invoke(invocation: ClientInvocation)

    /** Must return invocation of [action] ( which calls [invoke]). */
    @Throws(Exception::class)
    protected open fun executeInContext(action: () -> Any?): Any? = action()

    @SafeVarargs
    fun <C : Any> proxy(contractId: ContractId<C>, vararg interceptors: Interceptor): C {
        val interceptor = compositeInterceptor(*interceptors)
        return proxy(contractId) { method, arguments ->
            executeInContext {
                interceptor(method, arguments) {
                    val methodMapping = contractId.methodMapper.map(method)
                    val ready = if (methodMapping.oneWay) null else CountDownLatch(1)
                    var r: Reply? = null
                    invoke(object : ClientInvocation(methodMapping, arguments) {
                        override fun invoke(asyncSupported: Boolean, tunnel: Tunnel) =
                            tunnel(Request(contractId.id, methodMapping.id, arguments))

                        override fun settle(reply: Reply) {
                            if (ready == null) return // OneWay
                            r = reply
                            ready.countDown()
                        }
                    })
                    ready?.await()
                    r?.process()
                }
            }
        }
    }

    @JvmOverloads
    fun <C : Any> asyncProxy(contractId: ContractId<C>, interceptor: AsyncInterceptor = DirectAsyncInterceptor): C =
        proxy(contractId) { method, arguments ->
            val methodMapping = contractId.methodMapper.map(method)
            val promise = promise_.get()
            check((promise != null) || methodMapping.oneWay) {
                "asynchronous request/reply proxy call must be enclosed with 'promise' function"
            }
            check((promise == null) || !methodMapping.oneWay) {
                "asynchronous OneWay proxy call must not be enclosed with 'promise' function"
            }
            executeInContext {
                invoke(object : ClientInvocation(methodMapping, arguments) {
                    override fun invoke(asyncSupported: Boolean, tunnel: Tunnel) {
                        check(asyncSupported) { "asynchronous services not supported (service id ${contractId.id})" }
                        interceptor.entry(this)
                        tunnel(Request(contractId.id, methodMapping.id, arguments))
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
            }
            handlePrimitiveTypes(method.returnType)
        }
}

private val promise_ = ThreadLocal<CompletableFuture<Any>>()

fun <T : Any?> promise(execute: () -> T): CompletionStage<T> {
    val promise = CompletableFuture<T>()
    @Suppress("UNCHECKED_CAST") threadLocal(promise_, promise as CompletableFuture<Any>) { execute() }
    return promise
}

private val HandlePrimitiveTypes = mapOf(
    Boolean::class.javaPrimitiveType to false,
    Byte::class.javaPrimitiveType to 0.toByte(),
    Short::class.javaPrimitiveType to 0.toShort(),
    Int::class.javaPrimitiveType to 0,
    Long::class.javaPrimitiveType to 0.toLong(),
    Char::class.javaPrimitiveType to 0.toChar(),
    Float::class.javaPrimitiveType to 0.toFloat(),
    Double::class.javaPrimitiveType to 0.toDouble()
)

private fun handlePrimitiveTypes(type: Class<*>): Any? = HandlePrimitiveTypes[type]
