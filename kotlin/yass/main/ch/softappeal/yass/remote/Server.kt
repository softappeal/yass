package ch.softappeal.yass.remote

import ch.softappeal.yass.*

typealias ReplyWriter = (reply: Reply) -> Unit

abstract class AbstractService<C : Any> internal constructor(
    val contractId: ContractId<C>,
    internal val implementation: C
) {
    internal abstract fun invoke(invocation: AbstractInvocation, cleanup: () -> Unit, replyWriter: ReplyWriter)
}

class ServerInvocation internal constructor(
    val service: AbstractService<*>, request: Request
) : AbstractInvocation(service.contractId.methodMapper.map(request.methodId), request.arguments) {
    fun invoke(cleanup: () -> Unit, replyWriter: ReplyWriter) =
        service.invoke(this, cleanup, replyWriter)

    fun invoke(replyWriter: ReplyWriter) =
        invoke({}, replyWriter)
}

class Server @SafeVarargs constructor(vararg services: AbstractService<*>) {
    private val id2service = mutableMapOf<Int, AbstractService<*>>()

    init {
        for (service in services) {
            check(
                id2service.put(service.contractId.id, service) == null
            ) { "service id ${service.contractId.id} already added" }
        }
    }

    fun invocation(asyncSupported: Boolean, request: Request): ServerInvocation {
        val service = checkNotNull(id2service[request.serviceId]) {
            "no service id ${request.serviceId} found (method id ${request.methodId})"
        }
        check(asyncSupported || (service !is AsyncService<*>)) {
            "asynchronous services not supported (service id ${service.contractId.id})"
        }
        return ServerInvocation(service, request)
    }
}

val EmptyServer = Server()

class Service<C : Any> @SafeVarargs constructor(
    contractId: ContractId<C>, implementation: C, vararg interceptors: Interceptor
) : AbstractService<C>(contractId, implementation) {
    private val interceptor = compositeInterceptor(*interceptors)
    override fun invoke(invocation: AbstractInvocation, cleanup: () -> Unit, replyWriter: ReplyWriter) {
        val reply = try {
            ValueReply(invoke(interceptor, invocation.methodMapping.method, implementation, invocation.arguments))
        } catch (e: Exception) {
            if (invocation.methodMapping.oneWay) throw e
            ExceptionReply(e)
        }
        if (!invocation.methodMapping.oneWay) replyWriter(reply)
        cleanup()
    }
}

@OnlyNeededForJava
@SafeVarargs
fun <C : Any> service(contractId: ContractId<C>, implementation: C, vararg interceptors: Interceptor) =
    Service(contractId, implementation, *interceptors)

abstract class Completer {
    abstract fun complete(result: Any?)
    fun complete() = complete(null)
    abstract fun completeExceptionally(exception: Exception)
}

private val completer_ = ThreadLocal<Completer?>()

val completer: Completer
    get() = checkNotNull(completer_.get()) { "no active asynchronous request/reply service invocation" }

class AsyncService<C : Any>(
    contractId: ContractId<C>, implementation: C, private val interceptor: AsyncInterceptor = DirectAsyncInterceptor
) : AbstractService<C>(contractId, implementation) {
    override fun invoke(invocation: AbstractInvocation, cleanup: () -> Unit, replyWriter: ReplyWriter) {
        threadLocal(
            completer_,
            if (invocation.methodMapping.oneWay) null else object : Completer() {
                override fun complete(result: Any?) = try {
                    interceptor.exit(invocation, result)
                    replyWriter(ValueReply(result))
                } finally {
                    cleanup()
                }

                override fun completeExceptionally(exception: Exception) = try {
                    interceptor.exception(invocation, exception)
                    replyWriter(ExceptionReply(exception))
                } finally {
                    cleanup()
                }
            }
        ) {
            interceptor.entry(invocation)
            invoke(invocation.methodMapping.method, implementation, invocation.arguments)
        }
    }
}

@OnlyNeededForJava
@JvmOverloads
fun <C : Any> asyncService(
    contractId: ContractId<C>,
    implementation: C,
    interceptor: AsyncInterceptor = DirectAsyncInterceptor
) = AsyncService(contractId, implementation, interceptor)
