package ch.softappeal.yass.remote

import ch.softappeal.yass.*

abstract class SClient {
    protected abstract suspend fun invoke(request: Request, oneWay: Boolean): Reply?

    // $todo: intercept object methods
    @SafeVarargs
    fun <C : Any> proxy(contractId: ContractId<C>, vararg interceptors: SInterceptor): C =
        sProxy(contractId.contract.kotlin, sCompositeInterceptor(*interceptors)) { method, arguments ->
            val methodMapping = contractId.methodMapper.map(method)
            invoke(Request(contractId.id, methodMapping.id, arguments), methodMapping.oneWay)?.process()
        }
}
