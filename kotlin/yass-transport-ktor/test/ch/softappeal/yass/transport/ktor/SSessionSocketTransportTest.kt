package ch.softappeal.yass.transport.ktor

import ch.softappeal.yass.remote.session.*
import ch.softappeal.yass.remote.session.TestServiceId
import ch.softappeal.yass.transport.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import kotlinx.coroutines.*
import java.net.*
import kotlin.system.*
import kotlin.test.*

private val PacketSerializer = sPacketSerializer(MessageSerializer)

@KtorExperimentalAPI
class SessionSocketTransportTest {
    private val tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
    private val address = InetSocketAddress("127.0.0.1", 2323)

    @Test
    fun test() = try {
        class ASession : SSession() {
            override fun server() = server(this)
            override fun opened() {
                GlobalScope.launch {
                    println("$this ${(connection as SSocketConnection).socket.remoteAddress} opened")
                    val testService = proxy(TestServiceId)
                    assertEquals("hello", testService.echo("hello"))
                    testService.noResult()
                    testService.ping()
                    delay(10)
                    close()
                    assertTrue(isClosed)
                }
            }

            override suspend fun closed(exception: Exception?) {
                println("$this ${Thread.currentThread()} closed $exception")
            }
        }
        runBlocking {
            val acceptorJob = sStartSocketAcceptor(
                this, tcp.bind(address), SAcceptorSetup(PacketSerializer) { ASession() }
            )
            repeat(2) {
                sStartSocketInitiator(SInitiatorSetup(PacketSerializer) { ASession() }) { tcp.connect(address) }
            }
            delay(300)
            acceptorJob.cancel()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    @Test
    fun performance() = try {
        class BSession(private val action: Boolean) : SSession() {
            override fun server() = server(this)
            override fun opened() {
                if (!action) return
                GlobalScope.launch {
                    val testService = proxy(TestServiceId)
                    repeat(2) {
                        println(measureTimeMillis {
                            repeat(100) { assertEquals("hello", testService.echo("hello")) }
                        })
                    }
                    delay(100)
                    close()
                }
            }

            override suspend fun closed(exception: Exception?) {
                println("$this ${Thread.currentThread()} closed $exception")
            }
        }
        runBlocking {
            val acceptorJob = sStartSocketAcceptor(
                this, tcp.bind(address), SAcceptorSetup(PacketSerializer) { BSession(false) }
            )
            sStartSocketInitiator(SInitiatorSetup(PacketSerializer) { BSession(true) }) { tcp.connect(address) }.join()
            acceptorJob.cancel()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    @Test
    fun failedOpen() = try {
        class CSession(private val fail: Boolean) : SSession() {
            override fun opened() {
                if (fail) throw Exception("failed")
            }

            override suspend fun closed(exception: Exception?) {
                println("closed: $exception")
            }
        }
        runBlocking {
            val acceptorJob = sStartSocketAcceptor(
                GlobalScope, tcp.bind(address), SAcceptorSetup(PacketSerializer) { CSession(false) }
            )
            repeat(2) {
                GlobalScope.sStartSocketInitiator(SInitiatorSetup(PacketSerializer) { CSession(true) }) {
                    tcp.connect(address)
                }
            }
            delay(400)
            println("canceling job ...")
            assertTrue(acceptorJob.isActive)
            acceptorJob.cancel()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
