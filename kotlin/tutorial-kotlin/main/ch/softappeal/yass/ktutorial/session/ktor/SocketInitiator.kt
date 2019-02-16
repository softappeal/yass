package ch.softappeal.yass.ktutorial.session.ktor

import ch.softappeal.yass.ktutorial.session.*
import ch.softappeal.yass.transport.*
import ch.softappeal.yass.transport.ktor.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import kotlinx.coroutines.*
import java.net.*

val Address = InetSocketAddress("127.0.0.1", 23237)

@KtorExperimentalAPI
val Tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()

@KtorExperimentalAPI
fun main() {
    runBlocking {
        sStartSocketInitiator(SInitiatorSetup(PacketSerializer) {
            object : InitiatorSession() {
                override fun connectionContext() = (connection as SSocketConnection).socket.remoteAddress.toString()
            }
        }) { Tcp.connect(Address) }
    }
}
