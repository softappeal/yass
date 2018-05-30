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
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

val PRINTER: Interceptor = { _, _, invocation ->
    val socket = socket()
    assertNotNull(socket)
    println("$socket")
    invocation()
}

val messageSerializer = messageSerializer(JavaSerializer)

class SocketTransportTest {

    @Test
    fun noSocket() {
        try {
            socket()
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("no active invocation", e.message)
        }
    }

    @Test
    fun test() = useExecutor { executor, done ->
        val server = Server(Service(calculatorId, CalculatorImpl, PRINTER, serverPrinter))
        socketServer(ServerSetup(server, messageSerializer), executor)
            .start(executor, socketBinder(address)).use {
                useClient(
                    socketClient(ClientSetup(messageSerializer), socketConnector(address))
                        .proxy(calculatorId, PRINTER, clientPrinter)
                )
            }
        done()
    }

    @Test
    fun performance() = useExecutor { executor, done ->
        val server = Server(Service(calculatorId, CalculatorImpl))
        socketServer(ServerSetup(server, messageSerializer), executor)
            .start(executor, socketBinder(address)).use {
                performance(socketClient(ClientSetup(messageSerializer), socketConnector(address)))
            }
        done()
    }

}
