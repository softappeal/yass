package ch.softappeal.yass.remote

import ch.softappeal.yass.*

class SServiceInvocation internal constructor(private val service: SService<*>, internal val request: Request) {
    internal val methodMapping = service.contractId.methodMapper.map(request.methodId)
    val oneWay = methodMapping.oneWay
    suspend fun invoke(): Reply = service.invoke(this)
}

class SServer @SafeVarargs constructor(vararg services: SService<*>) {
    private val id2service = mutableMapOf<Int, SService<*>>()

    init {
        for (service in services)
            require(id2service.put(service.contractId.id, service) == null) {
                "service id ${service.contractId.id} already added"
            }
    }

    fun invocation(request: Request) = SServiceInvocation(checkNotNull(id2service[request.serviceId]) {
        "no service id ${request.serviceId} found (method id ${request.methodId})"
    }, request)
}

class SService<C : Any> @SafeVarargs constructor(
    internal val contractId: ContractId<C>, private val implementation: C, vararg interceptors: SInterceptor
) {
    private val interceptor = sCompositeInterceptor(*interceptors)
    internal suspend fun invoke(invocation: SServiceInvocation) = with(invocation) {
        try {
            ValueReply(methodMapping.method.sInvoke(interceptor, implementation, request.arguments))
        } catch (e: Exception) {
            if (oneWay) throw e
            ExceptionReply(e)
        }
    }
}
