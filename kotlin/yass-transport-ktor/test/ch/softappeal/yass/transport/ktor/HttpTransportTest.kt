package ch.softappeal.yass.transport.ktor

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.transport.*
import io.ktor.client.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.test.*
import io.ktor.client.engine.cio.CIO as CCIO
import io.ktor.server.cio.CIO as SCIO

private val CallInterceptor: SInterceptor = { _, _, invocation ->
    println(coroutineContext[CallCCE]?.call?.request?.uri)
    invocation()
}

@KtorExperimentalAPI
class HttpTransportTest {
    private val client = httpClient(MessageSerializer, "http://localhost:8080/yass") { HttpClient(CCIO) }

    @Test
    fun test() = try {
        embeddedServer(SCIO, 8080) {
            routing {
                route(
                    "/yass",
                    SServerTransport(
                        SServer(SService(TestServiceId, TestServiceImpl, CallInterceptor, ServerPrinter)),
                        MessageSerializer
                    )
                )
            }
        }.start(wait = false)
        val testService = client.proxy(TestServiceId, ClientPrinter)
        runBlocking {
            testService.test()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    @Test
    fun performance() = try {
        embeddedServer(SCIO, 8080) {
            routing {
                route(
                    "/yass",
                    SServerTransport(
                        SServer(SService(TestServiceId, TestServiceImpl)),
                        MessageSerializer
                    )
                )
            }
        }.start(wait = false)
        val testService = client.proxy(TestServiceId)
        runBlocking {
            testService.performance(1)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
