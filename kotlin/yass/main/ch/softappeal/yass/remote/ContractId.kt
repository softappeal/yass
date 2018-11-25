package ch.softappeal.yass.remote

import ch.softappeal.yass.OnlyNeededForJava

class ContractId<C : Any> @PublishedApi internal constructor(
    val contract: Class<C>,
    val id: Int,
    val methodMapper: MethodMapper
)

inline fun <reified C : Any> contractId(id: Int, methodMapperFactory: MethodMapperFactory): ContractId<C> =
    ContractId(C::class.java, id, methodMapperFactory(C::class.java))

@OnlyNeededForJava
fun <C : Any> contractId(contract: Class<C>, id: Int, methodMapperFactory: MethodMapperFactory): ContractId<C> =
    ContractId(contract, id, methodMapperFactory(contract))

abstract class Services protected constructor(val methodMapperFactory: MethodMapperFactory) {
    @PublishedApi
    internal val identifiers = mutableSetOf<Int>()

    @OnlyNeededForJava
    protected fun <C : Any> contractId(contract: Class<C>, id: Int): ContractId<C> {
        require(identifiers.add(id)) { "service with id $id already added" }
        return contractId(contract, id, methodMapperFactory)
    }

    protected inline fun <reified C : Any> contractId(id: Int): ContractId<C> =
        contractId(C::class.java, id)
}

abstract class AbstractInvocation internal constructor(val methodMapping: MethodMapping, val arguments: List<Any?>) {
    @Volatile
    var context: Any? = null
}

interface AsyncInterceptor {
    @Throws(Exception::class)
    fun entry(invocation: AbstractInvocation)

    @Throws(Exception::class)
    fun exit(invocation: AbstractInvocation, result: Any?)

    @Throws(Exception::class)
    fun exception(invocation: AbstractInvocation, exception: Exception)
}

val DirectAsyncInterceptor = object : AsyncInterceptor {
    override fun entry(invocation: AbstractInvocation) {}
    override fun exit(invocation: AbstractInvocation, result: Any?) {}
    override fun exception(invocation: AbstractInvocation, exception: Exception) {}
}
