package ch.softappeal.yass.transport.ktor

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.transport.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import kotlinx.coroutines.*
import java.net.*
import kotlin.coroutines.*
import kotlin.test.*

private val SocketInterceptor: SInterceptor = { _, _, invocation ->
    println(coroutineContext[SocketCCE]?.socket?.remoteAddress)
    invocation()
}

@KtorExperimentalAPI
class SocketTransportTest {
    private val tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
    private val address = InetSocketAddress("127.0.0.1", 2323)
    private val client = sSocketClient(SClientSetup(MessageSerializer)) { tcp.connect(address) }

    @Test
    fun test() = try {
        runBlocking {
            val job = sStartSocketServer(
                this, tcp.bind(address), SServerSetup(
                    SServer(SService(TestServiceId, TestServiceImpl, SocketInterceptor, ServerPrinter)),
                    MessageSerializer
                )
            )
            val testService = client.proxy(TestServiceId, ClientPrinter)
            testService.test()
            job.cancel()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    @Test
    fun performance() = try {
        runBlocking {
            val job = sStartSocketServer(
                this, tcp.bind(address), SServerSetup(
                    SServer(SService(TestServiceId, TestServiceImpl)),
                    MessageSerializer
                )
            )
            val testService = client.proxy(TestServiceId)
            testService.performance(1)
            job.cancel()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
