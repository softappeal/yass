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
    println("$message - thread ${Thread.currentThread().id} : context ${Serializer.context}")
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
                assertNull(Serializer.context)
                assertEquals(3, calculator.divide(12, 4))
                assertNull(Serializer.context)
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
                    Serializer.context = "server"
                }
            },
            { _, _, invocation ->
                Serializer.context = "client"
                try {
                    invocation()
                } finally {
                    print("client")
                    Serializer.context = null
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
                    Serializer.context = null
                }
            },
            { _, _, invocation ->
                Serializer.context = "client"
                try {
                    invocation()
                } finally {
                    Serializer.context = null
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
                    Serializer.context = "server"
                }
            },
            { _, _, invocation ->
                try {
                    invocation()
                } finally {
                    print("client")
                    Serializer.context = null
                }
            }
        )
    }
}
