@file:Suppress("UNUSED_PARAMETER", "CAST_NEVER_SUCCEEDS")

package ch.softappeal.yass.doc

import java.util.concurrent.CompletionStage

class ContractId<C : Any>(val contract: Class<C>, val id: Int)

inline fun <reified C : Any> contractId(id: Int): ContractId<C> = null as ContractId<C>

abstract class Client {
    fun <C : Any> proxy(contractId: ContractId<C>): C = null as C
    fun <C : Any> asyncProxy(contractId: ContractId<C>): C = null as C
}

fun <T : Any?> promise(execute: () -> T): CompletionStage<T> = null as CompletionStage<T>

class Server(vararg services: AbstractService<*>)

abstract class AbstractService<C : Any>

class Service<C : Any>(contractId: ContractId<C>, implementation: C) : AbstractService<C>()

class AsyncService<C : Any>(contractId: ContractId<C>, implementation: C) : AbstractService<C>()

abstract class Completer {
    abstract fun complete(result: Any?)
    fun complete() = complete(null)
    abstract fun completeExceptionally(exception: Exception)
}

val completer: Completer get() = null as Completer

abstract class Session : Client(), AutoCloseable {
    val isClosed get() = true
    protected open fun server(): Server = null as Server
    protected open fun opened() {}
    protected open fun closed(exception: Exception?) {}
    override fun close() {}
}
