package ch.softappeal.yass.transport.socket

import ch.softappeal.yass.Interceptor
import ch.softappeal.yass.remote.CalculatorImpl
import ch.softappeal.yass.remote.Server
import ch.softappeal.yass.remote.Service
import ch.softappeal.yass.remote.calculatorId
import ch.softappeal.yass.remote.session.useExecutor
import ch.softappeal.yass.serialize.JavaSerializer
import ch.softappeal.yass.transport.ClientSetup
import ch.softappeal.yass.transport.ContextMessageSerializer
import ch.softappeal.yass.transport.ServerSetup
import ch.softappeal.yass.transport.messageSerializer
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val Serializer = ContextMessageSerializer(JavaSerializer, messageSerializer(JavaSerializer))

private fun print(message: String) {
    println("$message - thread ${Thread.currentThread().id} : context ${Serializer.context}")
}

private fun test(serverInterceptor: Interceptor, clientInterceptor: Interceptor) {
    useExecutor { executor, done ->
        socketServer(ServerSetup(Server(Service(calculatorId, CalculatorImpl, serverInterceptor)), Serializer), executor)
            .start(executor, socketBinder(address)).use {
                TimeUnit.MILLISECONDS.sleep(200L)
                val calculator = socketClient(ClientSetup(Serializer), socketConnector(address)).proxy(calculatorId, clientInterceptor)
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
                Serializer.context = null
                invocation()
            },
            { _, _, invocation ->
                Serializer.context = "client"
                invocation()
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
