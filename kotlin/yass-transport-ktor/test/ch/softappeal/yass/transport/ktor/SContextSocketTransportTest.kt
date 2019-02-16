package ch.softappeal.yass.transport.ktor

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.fast.*
import ch.softappeal.yass.transport.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import kotlinx.coroutines.*
import java.net.*
import kotlin.test.*

private val ContractSerializer = sSimpleFastSerializer(listOf(SIntSerializer, SStringSerializer), listOf())

private val ContextMessageSerializer =
    SContextMessageSerializer(ContractSerializer, sMessageSerializer(ContractSerializer))

private suspend fun print(message: String) {
    println("$message - thread ${Thread.currentThread().id} : context ${contextCCE().context}")
}

interface Calculator {
    suspend fun add(a: Int, b: Int): Int
}

class CalculatorImpl : Calculator {
    override suspend fun add(a: Int, b: Int) = a + b
}

private val CalculatorId = contractId<Calculator>(1, SimpleMethodMapperFactory)

@KtorExperimentalAPI
class SContextSocketTransportTest {
    private fun test(serverInterceptor: SInterceptor, clientInterceptor: SInterceptor) {
        val tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
        val address = InetSocketAddress("localhost", 28947)
        runBlocking {
            val serverJob = sStartSocketServer(
                CoroutineScope(GlobalScope.coroutineContext + ContextCCE()),
                tcp.bind(address),
                SServerSetup(
                    SServer(SService(CalculatorId, CalculatorImpl(), serverInterceptor)),
                    ContextMessageSerializer
                )
            )
            val calculator = sSocketClient(SClientSetup(ContextMessageSerializer)) { tcp.connect(address) }
                .proxy(CalculatorId, clientInterceptor)
            withContext(ContextCCE()) {
                assertNull(contextCCE().context)
                assertEquals(3, calculator.add(1, 2))
                assertNull(contextCCE().context)
            }
            serverJob.cancel()
        }
    }

    @Test
    fun bidirectional() {
        test(
            { _, _, invocation ->
                print("server")
                try {
                    invocation()
                } finally {
                    contextCCE().context = "server"
                }
            },
            { _, _, invocation ->
                contextCCE().context = "client"
                try {
                    invocation()
                } finally {
                    print("client")
                    contextCCE().context = null
                }
            }
        )
    }

    @Test
    fun client2server() {
        test(
            { _, _, invocation ->
                print("server")
                try {
                    invocation()
                } finally {
                    contextCCE().context = null
                }
            },
            { _, _, invocation ->
                contextCCE().context = "client"
                try {
                    invocation()
                } finally {
                    contextCCE().context = null
                }
            }
        )
    }

    @Test
    fun server2client() {
        test(
            { _, _, invocation ->
                try {
                    invocation()
                } finally {
                    contextCCE().context = "server"
                }
            },
            { _, _, invocation ->
                try {
                    invocation()
                } finally {
                    print("client")
                    contextCCE().context = null
                }
            }
        )
    }
}
