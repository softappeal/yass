package ch.softappeal.yass.ktutorial.helloworld

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.fast.*
import ch.softappeal.yass.transport.*
import ch.softappeal.yass.transport.ktor.*
import io.ktor.client.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.util.*
import kotlinx.coroutines.*
import java.net.*
import io.ktor.client.engine.cio.CIO as CCIO
import io.ktor.server.cio.CIO as SCIO

interface Calculator {
    suspend fun add(a: Int, b: Int): Int
}

class CalculatorImpl : Calculator {
    override suspend fun add(a: Int, b: Int) = a + b
}

suspend fun useCalculator(calculator: Calculator) {
    println("2 + 3 = " + calculator.add(2, 3))
}

val CalculatorId = contractId<Calculator>(0, SimpleMethodMapperFactory)

val Server = SServer(
    SService(CalculatorId, CalculatorImpl())
)

val ContractSerializer = sSimpleFastSerializer(listOf(SIntSerializer), listOf())

val MessageSerializer = sMessageSerializer(ContractSerializer)

@KtorExperimentalAPI
fun main() {

    fun socket() {
        val tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
        val address = InetSocketAddress("localhost", 28947)
        runBlocking {
            val serverJob = sStartSocketServer(this, tcp.bind(address), SServerSetup(Server, MessageSerializer))
            val client = sSocketClient(SClientSetup(MessageSerializer)) { tcp.connect(address) }
            useCalculator(client.proxy(CalculatorId))
            serverJob.cancel()
        }
    }

    fun http() {
        val port = 8080
        embeddedServer(SCIO, port) {
            routing {
                route("/", SServerTransport(Server, MessageSerializer))
            }
        }.start()
        runBlocking {
            val client = httpClient(MessageSerializer, "http://localhost:$port/") { HttpClient(CCIO) }
            useCalculator(client.proxy(CalculatorId))
        }
    }

    socket()
    http()
}
