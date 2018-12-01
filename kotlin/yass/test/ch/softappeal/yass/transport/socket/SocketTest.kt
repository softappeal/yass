package ch.softappeal.yass.transport.socket

import java.net.*
import kotlin.test.*

val address = InetSocketAddress("localhost", 28947)

class SocketTest {
    @Test
    fun failedConnect() {
        assertFailsWith<ConnectException> { socketConnector(address)() }
    }

    @Test
    fun failedFirstSocketConnector() {
        assertEquals(
            "all connectors failed",
            assertFailsWith<IllegalStateException> {
                firstSocketConnector()()
            }.message
        )
        assertEquals(
            "all connectors failed",
            assertFailsWith<IllegalStateException> {
                firstSocketConnector(socketConnector(address))()
            }.message
        )
    }

    @Test
    fun failedBind() {
        val socketBinder = socketBinder(address)
        socketBinder().use { serverSocket ->
            println(serverSocket)
            assertFailsWith<BindException> { socketBinder() }
        }
    }
}
