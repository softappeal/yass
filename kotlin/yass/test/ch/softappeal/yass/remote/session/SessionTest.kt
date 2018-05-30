package ch.softappeal.yass.remote.session

import ch.softappeal.yass.Terminate
import ch.softappeal.yass.namedThreadFactory
import ch.softappeal.yass.remote.CalculatorImpl
import ch.softappeal.yass.remote.Server
import ch.softappeal.yass.remote.Service
import ch.softappeal.yass.remote.calculatorId
import ch.softappeal.yass.remote.clientPrinter
import ch.softappeal.yass.remote.performance
import ch.softappeal.yass.remote.serverPrinter
import ch.softappeal.yass.remote.useClient
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.test.assertTrue
import kotlin.test.fail

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
        executor.shutdown()
    }
}

fun createTestSession(
    dispatchExecutor: Executor, done: (() -> Unit)?, connectionHandler: (connection: Connection) -> Unit = {}
) = object : SimpleSession(dispatchExecutor) {
    init {
        assertTrue(isClosed)
        try {
            proxy(calculatorId).oneWay()
            fail()
        } catch (ignore: SessionClosedException) {
        }
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
        try {
            proxy(calculatorId).twoWay()
            fail()
        } catch (ignore: SessionClosedException) {
        }
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
}
