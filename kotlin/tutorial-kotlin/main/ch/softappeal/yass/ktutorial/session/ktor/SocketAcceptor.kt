package ch.softappeal.yass.ktutorial.session.ktor

import ch.softappeal.yass.ktutorial.session.*
import ch.softappeal.yass.transport.*
import ch.softappeal.yass.transport.ktor.*
import io.ktor.util.*
import kotlinx.coroutines.*

@KtorExperimentalAPI
fun main() {
    runBlocking {
        sStartSocketAcceptor(GlobalScope, Tcp.bind(Address), SAcceptorSetup(PacketSerializer) {
            object : AcceptorSession() {
                override fun connectionContext() = (connection as SSocketConnection).socket.remoteAddress.toString()
            }
        })
    }
}
