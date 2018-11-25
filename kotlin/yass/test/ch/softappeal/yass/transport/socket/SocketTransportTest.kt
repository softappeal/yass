package ch.softappeal.yass.transport.socket

import ch.softappeal.yass.Interceptor
import ch.softappeal.yass.remote.CalculatorImpl
import ch.softappeal.yass.remote.Server
import ch.softappeal.yass.remote.Service
import ch.softappeal.yass.remote.calculatorId
import ch.softappeal.yass.remote.clientPrinter
import ch.softappeal.yass.remote.performance
import ch.softappeal.yass.remote.serverPrinter
import ch.softappeal.yass.remote.session.useExecutor
import ch.softappeal.yass.remote.useClient
import ch.softappeal.yass.serialize.JavaSerializer
import ch.softappeal.yass.transport.ClientSetup
import ch.softappeal.yass.transport.ServerSetup
import ch.softappeal.yass.transport.messageSerializer
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

private val Printer: Interceptor = { _, _, invocation ->
    val socket = socket
    assertNotNull(socket)
    println("$socket")
    invocation()
}

val messageSerializer = messageSerializer(JavaSerializer)

class SocketTransportTest {
    @Test
    fun noSocket() {
        try {
            socket
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("no active invocation", e.message)
        }
    }

    @Test
    fun test() {
        useExecutor { executor, done ->
            val server = Server(Service(calculatorId, CalculatorImpl, Printer, serverPrinter))
            socketServer(ServerSetup(server, messageSerializer), executor)
                .start(executor, socketBinder(address)).use {
                    TimeUnit.MILLISECONDS.sleep(200L)
                    useClient(
                        socketClient(ClientSetup(messageSerializer), socketConnector(address))
                            .proxy(calculatorId, Printer, clientPrinter)
                    )
                }
            done()
        }
        TimeUnit.MILLISECONDS.sleep(200L)
    }

    @Test
    fun performance() {
        useExecutor { executor, done ->
            val server = Server(Service(calculatorId, CalculatorImpl))
            socketServer(ServerSetup(server, messageSerializer), executor)
                .start(executor, socketBinder(address)).use {
                    TimeUnit.MILLISECONDS.sleep(200L)
                    performance(socketClient(ClientSetup(messageSerializer), socketConnector(address)))
                }
            done()
        }
        TimeUnit.MILLISECONDS.sleep(200L)
    }

    @Test
    fun firstSocketConnector() {
        useExecutor { executor, done ->
            val server = Server(Service(calculatorId, CalculatorImpl, Printer, serverPrinter))
            socketServer(ServerSetup(server, messageSerializer), executor)
                .start(executor, socketBinder(address)).use {
                    TimeUnit.MILLISECONDS.sleep(200L)
                    useClient(
                        socketClient(
                            ClientSetup(messageSerializer), firstSocketConnector(
                                socketConnector(
                                    InetSocketAddress("localhost", 28948),
                                    connectTimeoutMilliSeconds = 1
                                ),
                                socketConnector(address)
                            )
                        )
                            .proxy(calculatorId, Printer, clientPrinter)
                    )
                }
            done()
        }
        TimeUnit.MILLISECONDS.sleep(200L)
    }
}
