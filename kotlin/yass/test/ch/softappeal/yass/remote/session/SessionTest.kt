package ch.softappeal.yass.remote.session

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import java.util.concurrent.*
import kotlin.test.*

fun useExecutor(
    uncaughtExceptionHandler: Thread.UncaughtExceptionHandler = Terminate,
    test: (executor: Executor, done: () -> Unit) -> Unit
) {
    val done = CountDownLatch(1)
    val executor = Executors.newCachedThreadPool(namedThreadFactory("test", uncaughtExceptionHandler))
    try {
        test(executor) { done.countDown() }
        done.await()
    } finally {
        executor.shutdownNow()
    }
}

fun createTestSession(
    dispatchExecutor: Executor, done: (() -> Unit)?, connectionHandler: (connection: Connection) -> Unit = {}
) = object : SimpleSession(dispatchExecutor) {
    init {
        assertTrue(isClosed)
        assertFailsWith<SessionClosedException> { proxy(calculatorId).oneWay() }
        println("init")
    }

    override fun server(): Server {
        assertTrue(isClosed)
        return Server(Service(calculatorId, CalculatorImpl, serverPrinter))
    }

    override fun opened() {
        println("opened")
        connectionHandler(connection)
        if (done == null) return
        useClient(proxy(calculatorId, clientPrinter))
        close()
        assertFailsWith<SessionClosedException> { proxy(calculatorId).twoWay() }
        println("done")
        done()
    }

    override fun closed(exception: Exception?) {
        assertTrue(isClosed)
        println("closed")
        exception?.printStackTrace()
    }
}

private fun connect(session1: Session, session2: Session) {
    class LocalConnection(private val session: Session) : Connection {
        override fun write(packet: Packet) = session.received(packet)
        override fun closed() = session.close()
    }
    session1.created(LocalConnection(session2))
    session2.created(LocalConnection(session1))
}

class LocalConnectionTest {
    @Test
    fun test() = useExecutor { executor, done ->
        connect(createTestSession(executor, done), createTestSession(executor, null))
    }

    @Test
    fun performanceTest() = useExecutor { executor, done ->
        val serverSession = object : SimpleSession(executor) {
            override fun server() = Server(Service(calculatorId, CalculatorImpl))
        }
        val clientSession = object : SimpleSession(executor) {
            override fun opened() {
                performance(this)
                done()
            }
        }
        connect(serverSession, clientSession)
    }

    @Test
    fun watcherTest1() = useExecutor { executor, done ->
        val serverSession = SimpleSession(executor)
        val clientSession = object : SimpleSession(executor) {
            override fun closed(exception: Exception?) {
                println(exception)
            }
        }
        connect(serverSession, clientSession)
        assertFalse(clientSession.isClosed)
        var failed = false
        watchSession(executor, clientSession, 1, 2) {
            println("check")
            if (failed) throw Exception("failed")
        }
        TimeUnit.SECONDS.sleep(3L)
        failed = true
        TimeUnit.SECONDS.sleep(3L)
        done()
        assertTrue(clientSession.isClosed)
    }

    @Test
    fun watcherTest2() = useExecutor { executor, done ->
        val serverSession = SimpleSession(executor)
        val clientSession = object : SimpleSession(executor) {
            override fun closed(exception: Exception?) {
                println(exception)
            }
        }
        connect(serverSession, clientSession)
        assertFalse(clientSession.isClosed)
        watchSession(executor, clientSession, 1, 1) {
            TimeUnit.SECONDS.sleep(2L)
        }
        TimeUnit.SECONDS.sleep(3L)
        done()
        assertTrue(clientSession.isClosed)
    }

    @Test
    fun reconnectorTest() {
        class InitiatorSession(dispatchExecutor: Executor) : SimpleSession(dispatchExecutor) {
            val calculator = proxy(calculatorId)
        }

        class AcceptorSession(dispatchExecutor: Executor) : SimpleSession(dispatchExecutor) {
            override fun server() = Server(Service(calculatorId, CalculatorImpl))
        }

        class InitiatorReconnector : Reconnector<InitiatorSession>() {
            val calculator = proxy { session -> session.calculator }
        }
        useExecutor { executor, done ->
            val reconnector = InitiatorReconnector()
            reconnector.start(executor, 1L, { InitiatorSession(executor) }) { sessionFactory ->
                println("connect")
                connect(AcceptorSession(executor), sessionFactory())
            }
            TimeUnit.MILLISECONDS.sleep(200L)
            assertTrue(reconnector.isConnected)
            assertEquals(4, reconnector.calculator.divide(12, 3))
            reconnector.session.close()
            assertFailsWith<SessionClosedException> { reconnector.calculator.divide(12, 3) }
            TimeUnit.SECONDS.sleep(2L)
            assertEquals(4, reconnector.calculator.divide(12, 3))
            done()
        }
    }
}
