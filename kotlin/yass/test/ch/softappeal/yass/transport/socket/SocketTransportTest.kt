package ch.softappeal.yass.transport.socket

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.remote.session.*
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.transport.*
import java.net.*
import java.util.concurrent.*
import kotlin.test.*

private val Printer: Interceptor = { _, _, invocation ->
    val socket = socket
    assertNotNull(socket)
    println("$socket")
    invocation()
}

private fun asyncInterceptor(side: String) = object : AsyncInterceptor {
    private val printer = asyncPrinter(side)
    override fun entry(invocation: AbstractInvocation) {
        printer.entry(invocation)
        val socket = socket
        assertNotNull(socket)
        println("$socket")
    }

    override fun exit(invocation: AbstractInvocation, result: Any?) {
        printer.exit(invocation, result)
    }

    override fun exception(invocation: AbstractInvocation, exception: Exception) {
        printer.exception(invocation, exception)
    }
}

val messageSerializer = messageSerializer(JavaSerializer)

class SocketTransportTest {
    @Test
    fun noSocket() = assertEquals(
        "no active invocation",
        assertFailsWith<IllegalStateException> { socket }.message
    )

    @Test
    fun syncClientSyncServer() {
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

    @Ignore // $$$ runs forever
    @Test
    fun syncClientAsyncServer() {
        useExecutor { executor, done ->
            val server = Server(AsyncService(calculatorId, AsyncCalculatorImpl, asyncInterceptor("server")))
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
