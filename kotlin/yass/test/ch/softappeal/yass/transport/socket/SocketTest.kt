package ch.softappeal.yass.transport.socket

import org.junit.Test
import java.net.BindException
import java.net.ConnectException
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.fail

val address = InetSocketAddress("localhost", 28947)

class SocketTest {
    @Test
    fun failedConnect() {
        try {
            socketConnector(address)()
            fail()
        } catch (ignored: ConnectException) {
        }
    }

    @Test
    fun failedFirstSocketConnector() {
        try {
            firstSocketConnector()()
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("all connectors failed", e.message)
        }
        try {
            firstSocketConnector(socketConnector(address))()
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("all connectors failed", e.message)
        }
    }

    @Test
    fun failedBind() {
        val socketBinder = socketBinder(address)
        socketBinder().use { serverSocket ->
            println(serverSocket)
            try {
                socketBinder()
                fail()
            } catch (ignored: BindException) {
            }
        }
    }
}
