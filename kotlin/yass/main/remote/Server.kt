package ch.softappeal.yass.remote

import ch.softappeal.yass.Interceptor
import ch.softappeal.yass.OnlyNeededForJava
import ch.softappeal.yass.compositeInterceptor
import ch.softappeal.yass.invoke

typealias ReplyWriter = (reply: Reply) -> Unit

abstract class AbstractService<C : Any> internal constructor(val contractId: ContractId<C>, internal val implementation: C) {
    internal abstract fun invoke(invocation: AbstractInvocation, replyWriter: ReplyWriter)
}

class ServerInvocation internal constructor(
    val service: AbstractService<*>, request: Request
) : AbstractInvocation(service.contractId.methodMapper.map(request.methodId), request.arguments) {
    fun invoke(replyWriter: ReplyWriter) =
        service.invoke(this, replyWriter)
}

class Server @SafeVarargs constructor(vararg services: AbstractService<*>) {
    private val id2service = mutableMapOf<Int, AbstractService<*>>()

    init {
        for (service in services) {
            check(id2service.put(service.contractId.id, service) == null) { "service id ${service.contractId.id} already added" }
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
    override fun invoke(invocation: AbstractInvocation, replyWriter: ReplyWriter) {
        replyWriter(
            try {
                ValueReply(invoke(interceptor, invocation.methodMapping.method, implementation, invocation.arguments))
            } catch (e: Exception) {
                if (invocation.methodMapping.oneWay) throw e
                ExceptionReply(e)
            }
        )
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

private val completer_ = ThreadLocal<Completer>()

val completer: Completer
    get() = checkNotNull(completer_.get()) { "no active asynchronous request/reply service invocation" }

class AsyncService<C : Any>(
    contractId: ContractId<C>, implementation: C, private val interceptor: AsyncInterceptor = DirectAsyncInterceptor
) : AbstractService<C>(contractId, implementation) {
    override fun invoke(invocation: AbstractInvocation, replyWriter: ReplyWriter) {
        val oldCompleter = completer_.get()
        completer_.set(if (invocation.methodMapping.oneWay) null else object : Completer() {
            override fun complete(result: Any?) {
                interceptor.exit(invocation, result)
                replyWriter(ValueReply(result))
            }

            override fun completeExceptionally(exception: Exception) {
                interceptor.exception(invocation, exception)
                replyWriter(ExceptionReply(exception))
            }
        })
        try {
            interceptor.entry(invocation)
            invoke(invocation.methodMapping.method, implementation, invocation.arguments)
        } finally {
            completer_.set(oldCompleter)
        }
    }
}

@OnlyNeededForJava
@JvmOverloads
fun <C : Any> asyncService(contractId: ContractId<C>, implementation: C, interceptor: AsyncInterceptor = DirectAsyncInterceptor) =
    AsyncService(contractId, implementation, interceptor)
