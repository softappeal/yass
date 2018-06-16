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
import ch.softappeal.yass.transport.readContextMessageSerializer
import ch.softappeal.yass.transport.readWriteContextMessageSerializer
import ch.softappeal.yass.transport.writeContextMessageSerializer
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun print(message: String, serializer: ContextMessageSerializer) {
    println("$message - thread ${Thread.currentThread().id} : context ${serializer.context}")
}

private fun test(
    serverSerializer: ContextMessageSerializer, serverInterceptor: Interceptor,
    clientSerializer: ContextMessageSerializer, clientInterceptor: Interceptor
) {
    useExecutor { executor, done ->
        socketServer(ServerSetup(Server(Service(calculatorId, CalculatorImpl, serverInterceptor)), serverSerializer), executor)
            .start(executor, socketBinder(address)).use {
                TimeUnit.MILLISECONDS.sleep(200L)
                val calculator = socketClient(ClientSetup(clientSerializer), socketConnector(address)).proxy(calculatorId, clientInterceptor)
                assertNull(clientSerializer.context)
                assertEquals(3, calculator.divide(12, 4))
                assertNull(clientSerializer.context)
            }
        done()
    }
    TimeUnit.MILLISECONDS.sleep(200L)
}

class ContextSocketTransportTest {
    @Test
    fun bidirectional() {
        val serializer = readWriteContextMessageSerializer(JavaSerializer, messageSerializer(JavaSerializer))
        test(
            serializer,
            { _, _, invocation ->
                print("server", serializer)
                try {
                    invocation()
                } finally {
                    serializer.context = "server"
                }
            },
            serializer,
            { _, _, invocation ->
                serializer.context = "client"
                try {
                    invocation()
                } finally {
                    print("client", serializer)
                    serializer.context = null
                }
            }
        )
    }

    @Test
    fun client2server() {
        val serverSerializer = readContextMessageSerializer(JavaSerializer, messageSerializer(JavaSerializer))
        val clientSerializer = writeContextMessageSerializer(JavaSerializer, messageSerializer(JavaSerializer))
        test(
            serverSerializer,
            { _, _, invocation ->
                print("server", serverSerializer)
                serverSerializer.context = null
                invocation()
            },
            clientSerializer,
            { _, _, invocation ->
                clientSerializer.context = "client"
                invocation()
            }
        )
    }

    @Test
    fun server2client() {
        val serverSerializer = writeContextMessageSerializer(JavaSerializer, messageSerializer(JavaSerializer))
        val clientSerializer = readContextMessageSerializer(JavaSerializer, messageSerializer(JavaSerializer))
        test(
            serverSerializer,
            { _, _, invocation ->
                try {
                    invocation()
                } finally {
                    serverSerializer.context = "server"
                }
            },
            clientSerializer,
            { _, _, invocation ->
                try {
                    invocation()
                } finally {
                    print("client", clientSerializer)
                    clientSerializer.context = null
                }
            }
        )
    }
}
