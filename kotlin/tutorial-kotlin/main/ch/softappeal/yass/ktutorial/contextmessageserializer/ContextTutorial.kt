package ch.softappeal.yass.ktutorial.contextmessageserializer

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.fast.*
import ch.softappeal.yass.transport.*
import ch.softappeal.yass.transport.socket.*
import java.net.*
import java.util.concurrent.*

class Context(val sequenceNumber: Int, val realm: String)

interface IdentityService {
    fun createIdentity(logonName: String)
}

val ContextSerializer = simpleFastSerializer(listOf(IntSerializer, StringSerializer), listOf(Context::class))
val ContractSerializer = simpleFastSerializer(listOf(StringSerializer), listOf())
val ContextMessageSerializer = ContextMessageSerializer(ContextSerializer, messageSerializer(ContractSerializer))

val IdentityServiceId = contractId<IdentityService>(1, SimpleMethodMapperFactory)

fun clientSide(client: Client) {
    val contextInterceptor: Interceptor = { _, _, invocation ->
        cmsContext = Context(123, "Google")
        try {
            invocation()
        } finally {
            cmsContext = null // cleanup if server returns context
        }
    }
    val identityService = client.proxy(IdentityServiceId, contextInterceptor)
    identityService.createIdentity("Bob")
}

typealias RealmGetter = () -> String

class IdentityServiceImpl(val realmGetter: RealmGetter) : IdentityService {
    override fun createIdentity(logonName: String) {
        println("creating identity $logonName for realm ${realmGetter()}")
    }
}

fun serverSide(): Service<IdentityService> {
    val contextInterceptor: Interceptor = { _, _, invocation ->
        println("logging sequenceNumber ${(cmsContext as Context).sequenceNumber}")
        try {
            invocation()
        } finally {
            cmsContext = null // server doesn't return context
        }
    }
    val realmGetter = { (cmsContext as Context).realm }
    return service(IdentityServiceId, IdentityServiceImpl(realmGetter), contextInterceptor)
}

fun main() {
    val address = InetSocketAddress("localhost", 28947)
    val executor = Executors.newCachedThreadPool(namedThreadFactory("executor", Terminate))
    try {
        socketServer(ServerSetup(Server(serverSide()), ContextMessageSerializer), executor)
            .start(executor, socketBinder(address))
            .use { clientSide(socketClient(ClientSetup(ContextMessageSerializer), socketConnector(address))) }
    } finally {
        executor.shutdown()
    }
}
