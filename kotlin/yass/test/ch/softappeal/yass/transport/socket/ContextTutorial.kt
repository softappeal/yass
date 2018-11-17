package ch.softappeal.yass.transport.socket

import ch.softappeal.yass.Interceptor
import ch.softappeal.yass.Terminate
import ch.softappeal.yass.namedThreadFactory
import ch.softappeal.yass.remote.Client
import ch.softappeal.yass.remote.Server
import ch.softappeal.yass.remote.Service
import ch.softappeal.yass.remote.SimpleMethodMapperFactory
import ch.softappeal.yass.remote.contractId
import ch.softappeal.yass.remote.service
import ch.softappeal.yass.serialize.fast.IntSerializer
import ch.softappeal.yass.serialize.fast.StringSerializer
import ch.softappeal.yass.serialize.fast.simpleFastSerializer
import ch.softappeal.yass.transport.ClientSetup
import ch.softappeal.yass.transport.ContextMessageSerializer
import ch.softappeal.yass.transport.ServerSetup
import ch.softappeal.yass.transport.messageSerializer
import java.net.InetSocketAddress
import java.util.concurrent.Executors

private class Context(val sequenceNumber: Int, val realm: String)

internal interface IdentityService {
    fun createIdentity(logonName: String)
}

private val CONTEXT_SERIALIZER = simpleFastSerializer(listOf(IntSerializer, StringSerializer), listOf(Context::class.java))
private val CONTRACT_SERIALIZER = simpleFastSerializer(listOf(StringSerializer), listOf())
private val CONTEXT = ContextMessageSerializer(CONTEXT_SERIALIZER, messageSerializer(CONTRACT_SERIALIZER))
private val IDENTITY_SERVICE = contractId<IdentityService>(1, SimpleMethodMapperFactory)
private val ADDRESS = InetSocketAddress("localhost", 28947)

private fun clientSide(client: Client) {
    val contextInterceptor: Interceptor = { _, _, invocation ->
        CONTEXT.context = Context(123, "Google")
        try {
            invocation()
        } finally {
            CONTEXT.context = null // cleanup if server returns context
        }
    }
    val identityService = client.proxy(IDENTITY_SERVICE, contextInterceptor)
    identityService.createIdentity("Bob")
}

private typealias RealmGetter = () -> String

private class IdentityServiceImpl(private val realmGetter: RealmGetter) : IdentityService {
    override fun createIdentity(logonName: String) {
        println("creating identity $logonName for realm ${realmGetter()}")
    }
}

private fun serverSide(): Service<IdentityService> {
    val contextInterceptor: Interceptor = { _, _, invocation ->
        println("logging sequenceNumber ${(CONTEXT.context as Context).sequenceNumber}")
        try {
            invocation()
        } finally {
            CONTEXT.context = null // server doesn't return context
        }
    }
    val realmGetter = { (CONTEXT.context as Context).realm }
    return service(IDENTITY_SERVICE, IdentityServiceImpl(realmGetter), contextInterceptor)
}

fun main() {
    val executor = Executors.newCachedThreadPool(namedThreadFactory("executor", Terminate))
    try {
        socketServer(ServerSetup(Server(serverSide()), CONTEXT), executor).start(executor, socketBinder(ADDRESS)).use {
            clientSide(socketClient(ClientSetup(CONTEXT), socketConnector(ADDRESS)))
        }
    } finally {
        executor.shutdown()
    }
}
