package ch.softappeal.yass.transport.socket

import ch.softappeal.yass.remote.CalculatorImpl
import ch.softappeal.yass.remote.Server
import ch.softappeal.yass.remote.Service
import ch.softappeal.yass.remote.calculatorId
import ch.softappeal.yass.remote.performance
import ch.softappeal.yass.remote.session.Connection
import ch.softappeal.yass.remote.session.SimpleSession
import ch.softappeal.yass.remote.session.createTestSession
import ch.softappeal.yass.remote.session.useExecutor
import ch.softappeal.yass.transport.AcceptorSetup
import ch.softappeal.yass.transport.InitiatorSetup
import ch.softappeal.yass.transport.packetSerializer
import org.junit.Test
import java.util.concurrent.TimeUnit

val packetSerializer = packetSerializer(messageSerializer)

class SessionSocketTransportTest {

    @Test
    fun test() {
        useExecutor { executor, done ->
            fun connectionHandler(connection: Connection) {
                println(connection)
                println((connection as SocketConnection).socket)
            }
            socketAcceptor(
                AcceptorSetup(packetSerializer) { createTestSession(executor, null, ::connectionHandler) },
                executor,
                asyncSocketConnectionFactory(executor, 1_000)
            ).start(executor, socketBinder(address)).use {
                socketInitiate(
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
                socketInitiate(
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
