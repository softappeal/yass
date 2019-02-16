package ch.softappeal.yass.ktutorial.scontextmessageserializer

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.fast.*
import ch.softappeal.yass.transport.*
import ch.softappeal.yass.transport.ktor.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import kotlinx.coroutines.*
import java.net.*

class Context(val sequenceNumber: Int, val realm: String)

interface IdentityService {
    suspend fun createIdentity(logonName: String)
}

val ContextSerializer = sSimpleFastSerializer(listOf(SIntSerializer, SStringSerializer), listOf(Context::class))
val ContractSerializer = sSimpleFastSerializer(listOf(SStringSerializer), listOf())
val ContextMessageSerializer = SContextMessageSerializer(ContextSerializer, sMessageSerializer(ContractSerializer))

val IdentityServiceId = contractId<IdentityService>(1, SimpleMethodMapperFactory)

suspend fun clientSide(client: SClient) {
    val contextInterceptor: SInterceptor = { _, _, invocation ->
        contextCCE().context = Context(123, "Google")
        try {
            invocation()
        } finally {
            contextCCE().context = null // cleanup if server returns context
        }
    }
    val identityService = client.proxy(IdentityServiceId, contextInterceptor)
    identityService.createIdentity("Bob")
}

typealias RealmGetter = suspend () -> String

class IdentityServiceImpl(val realmGetter: RealmGetter) : IdentityService {
    override suspend fun createIdentity(logonName: String) {
        println("creating identity $logonName for realm ${realmGetter()}")
    }
}

fun serverSide(): SService<IdentityService> {
    val contextInterceptor: SInterceptor = { _, _, invocation ->
        println("logging sequenceNumber ${(contextCCE().context as Context).sequenceNumber}")
        try {
            invocation()
        } finally {
            contextCCE().context = null // server doesn't return context
        }
    }
    val realmGetter: RealmGetter = { (contextCCE().context as Context).realm }
    return SService(IdentityServiceId, IdentityServiceImpl(realmGetter), contextInterceptor)
}

@KtorExperimentalAPI
fun main() {
    val tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
    val address = InetSocketAddress("localhost", 28947)
    runBlocking {
        val serverJob = sStartSocketServer(
            CoroutineScope(GlobalScope.coroutineContext + ContextCCE()),
            tcp.bind(address),
            SServerSetup(SServer(serverSide()), ContextMessageSerializer)
        )
        withContext(ContextCCE()) {
            clientSide(sSocketClient(SClientSetup(ContextMessageSerializer)) { tcp.connect(address) })
        }
        serverJob.cancel()
    }
}
