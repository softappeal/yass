package ch.softappeal.yass.ktutorial.contextmessageserializer

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.fast.*
import ch.softappeal.yass.transport.*
import ch.softappeal.yass.transport.socket.*
import java.net.*
import java.util.concurrent.*

private class Context(val sequenceNumber: Int, val realm: String)

internal interface IdentityService {
    fun createIdentity(logonName: String)
}

private val CONTEXT_SERIALIZER = simpleFastSerializer(listOf(IntSerializer, StringSerializer), listOf(Context::class))
private val CONTRACT_SERIALIZER = simpleFastSerializer(listOf(StringSerializer), listOf())
private val CONTEXT_MESSAGE_SERIALIZER =
    ContextMessageSerializer(CONTEXT_SERIALIZER, messageSerializer(CONTRACT_SERIALIZER))
private val IDENTITY_SERVICE_ID = contractId<IdentityService>(1, SimpleMethodMapperFactory)
private val ADDRESS = InetSocketAddress("localhost", 28947)

private fun clientSide(client: Client) {
    val contextInterceptor: Interceptor = { _, _, invocation ->
        CONTEXT_MESSAGE_SERIALIZER.context = Context(123, "Google")
        try {
            invocation()
        } finally {
            CONTEXT_MESSAGE_SERIALIZER.context = null // cleanup if server returns context
        }
    }
    val identityService = client.proxy(IDENTITY_SERVICE_ID, contextInterceptor)
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
        println("logging sequenceNumber ${(CONTEXT_MESSAGE_SERIALIZER.context as Context).sequenceNumber}")
        try {
            invocation()
        } finally {
            CONTEXT_MESSAGE_SERIALIZER.context = null // server doesn't return context
        }
    }
    val realmGetter = { (CONTEXT_MESSAGE_SERIALIZER.context as Context).realm }
    return service(IDENTITY_SERVICE_ID, IdentityServiceImpl(realmGetter), contextInterceptor)
}

fun main() {
    val executor = Executors.newCachedThreadPool(namedThreadFactory("executor", Terminate))
    try {
        socketServer(ServerSetup(Server(serverSide()), CONTEXT_MESSAGE_SERIALIZER), executor)
            .start(executor, socketBinder(ADDRESS))
            .use { clientSide(socketClient(ClientSetup(CONTEXT_MESSAGE_SERIALIZER), socketConnector(ADDRESS))) }
    } finally {
        executor.shutdown()
    }
}
