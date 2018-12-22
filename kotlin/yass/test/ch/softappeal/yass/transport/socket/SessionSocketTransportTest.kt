package ch.softappeal.yass.transport.socket

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.remote.session.*
import ch.softappeal.yass.transport.*
import java.util.concurrent.*
import kotlin.test.*

val packetSerializer = packetSerializer(messageSerializer)

class SessionSocketTransportTest {
    @Test
    fun invocations() {
        useExecutor { executor, done ->
            fun connectionHandler(connection: Connection) {
                println((connection as SocketConnection).socket)
            }
            socketAcceptor(
                AcceptorSetup(packetSerializer) { createTestSession(executor, null, ::connectionHandler) },
                executor,
                asyncSocketConnectionFactory(executor, 1_000)
            ).start(executor, socketBinder(address)).use {
                TimeUnit.MILLISECONDS.sleep(200L)
                socketInitiator(
                    InitiatorSetup(packetSerializer) { createTestSession(executor, done, ::connectionHandler) },
                    executor,
                    SyncSocketConnectionFactory,
                    socketConnector(address)
                )
            }
        }
        TimeUnit.MILLISECONDS.sleep(200L)
    }

    @Test
    fun performance() {
        useExecutor { executor, done ->
            socketAcceptor(
                AcceptorSetup(packetSerializer) {
                    object : SimpleSession(executor) {
                        override fun server() = Server(Service(calculatorId, CalculatorImpl))
                    }
                },
                executor,
                SyncSocketConnectionFactory
            ).start(executor, socketBinder(address)).use {
                TimeUnit.MILLISECONDS.sleep(200L)
                socketInitiator(
                    InitiatorSetup(packetSerializer) {
                        object : SimpleSession(executor) {
                            override fun opened() {
                                performance(this)
                                done()
                            }
                        }
                    },
                    executor,
                    SyncSocketConnectionFactory,
                    socketConnector(address)
                )
            }
        }
        TimeUnit.MILLISECONDS.sleep(200L)
    }
}
