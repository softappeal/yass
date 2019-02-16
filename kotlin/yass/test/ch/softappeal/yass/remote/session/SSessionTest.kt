package ch.softappeal.yass.remote.session

import ch.softappeal.yass.remote.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.test.*

interface TestService {
    suspend fun echo(value: String): String
    @OneWay
    suspend fun ping()

    @OneWay
    suspend fun pong()

    suspend fun noResult()
}

val TestServiceId = contractId<TestService>(1, SimpleMethodMapperFactory)

fun server(session: SSession): SServer {
    val testService = session.proxy(TestServiceId)
    return SServer(SService(TestServiceId, object : TestService {
        override suspend fun echo(value: String): String = value
        override suspend fun ping() {
            println("$session ${Thread.currentThread()} ping")
            testService.pong()
        }

        override suspend fun pong() {
            println("$session ${Thread.currentThread()} pong")
        }

        override suspend fun noResult() {
            println("$session ${Thread.currentThread()} noResult")
        }
    }))
}

private class TestSession : SSession() {
    override fun server() = server(this)
    override suspend fun closed(exception: Exception?) {
        println("$this $connection ${Thread.currentThread()} closed $exception")
    }
}

private class LocalConnection : SConnection {
    lateinit var other: SSession

    override suspend fun write(packet: Packet) {
        received(other, packet)
    }

    override suspend fun closed() {
        other.close()
    }

    override fun launch(coroutineContext: CoroutineContext, block: suspend () -> Unit) {
        CoroutineScope(coroutineContext).launch { block() }
    }
}

private fun connect(): Pair<TestSession, TestSession> {
    val connection1 = LocalConnection()
    val connection2 = LocalConnection()
    val session1 = TestSession()
    val session2 = TestSession()
    connection(session1, connection1)
    connection(session2, connection2)
    connection1.other = session2
    connection2.other = session1
    assertFalse(session1.isClosed)
    assertFalse(session2.isClosed)
    println(session1)
    println(session2)
    return Pair(session1, session2)
}

class SSessionTest {
    @Test
    fun test() {
        val (session1, session2) = connect()
        val testService1 = session1.proxy(TestServiceId)
        val testService2 = session2.proxy(TestServiceId)
        runBlocking {
            assertEquals("hello", testService1.echo("hello"))
            assertEquals("world", testService2.echo("world"))
            testService1.ping()
            testService2.ping()
            testService2.noResult()
            session1.close()
        }
        assertTrue(session1.isClosed)
        assertTrue(session2.isClosed)
    }

    @Test
    fun exception() {
        val (session1, session2) = connect()
        runBlocking { throwClose(session1, Exception("closed")) }
        assertTrue(session1.isClosed)
        assertTrue(session2.isClosed)
    }
}
