package ch.softappeal.yass.transport.socket

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.remote.session.*
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.transport.*
import java.util.concurrent.*
import kotlin.test.*

private val Serializer = ContextMessageSerializer(JavaSerializer, messageSerializer(JavaSerializer))

private fun print(message: String) {
    println("$message - thread ${Thread.currentThread().id} : context ${cmsContext}")
}

private fun test(serverInterceptor: Interceptor, clientInterceptor: Interceptor) {
    useExecutor { executor, done ->
        socketServer(
            ServerSetup(Server(Service(calculatorId, CalculatorImpl, serverInterceptor)), Serializer),
            executor
        )
            .start(executor, socketBinder(address)).use {
                TimeUnit.MILLISECONDS.sleep(200L)
                val calculator = socketClient(ClientSetup(Serializer), socketConnector(address))
                    .proxy(calculatorId, clientInterceptor)
                assertNull(cmsContext)
                assertEquals(3, calculator.divide(12, 4))
                assertNull(cmsContext)
            }
        done()
    }
    TimeUnit.MILLISECONDS.sleep(200L)
}

class ContextSocketTransportTest {
    @Test
    fun bidirectional() {
        test(
            { _, _, invocation ->
                print("server")
                try {
                    invocation()
                } finally {
                    cmsContext = "server"
                }
            },
            { _, _, invocation ->
                cmsContext = "client"
                try {
                    invocation()
                } finally {
                    print("client")
                    cmsContext = null
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
                    cmsContext = null
                }
            },
            { _, _, invocation ->
                cmsContext = "client"
                try {
                    invocation()
                } finally {
                    cmsContext = null
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
                    cmsContext = "server"
                }
            },
            { _, _, invocation ->
                try {
                    invocation()
                } finally {
                    print("client")
                    cmsContext = null
                }
            }
        )
    }
}
