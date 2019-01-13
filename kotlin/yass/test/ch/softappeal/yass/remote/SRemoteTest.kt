package ch.softappeal.yass.remote

import ch.softappeal.yass.*
import kotlinx.coroutines.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import kotlin.concurrent.*
import kotlin.coroutines.*
import kotlin.test.*

private val DividerId = contractId<Divider>(1, SimpleMethodMapperFactory)

private var counter = AtomicInteger(0)

private val TestInterceptor: SInterceptor = { method, _, invocation ->
    println("${method.name} ${Thread.currentThread().name} ${coroutineContext.hashCode()}")
    counter.incrementAndGet()
    invocation()
}

private val server = SServer(SService(DividerId, DividerImpl, TestInterceptor))

private val client = object : SClient() {
    override suspend fun invoke(request: Request, oneWay: Boolean): Reply? {
        println("client.oneWay: $oneWay")
        var reply: Reply? = null
        val ready = CountDownLatch(1)
        thread {
            runBlocking {
                val invocation = server.invocation(request)
                println("server.oneWay: ${invocation.oneWay}")
                reply = invocation.invoke()
                if (!invocation.oneWay) ready.countDown()
            }
        }
        if (oneWay) return null
        ready.await()
        return reply!!
    }
}

private val divider = client.proxy(DividerId, TestInterceptor)

class SRemoteTest {
    @Test
    fun services() {
        val service = SService(DividerId, DividerImpl)
        assertEquals(
            "service id 1 already added",
            assertFailsWith<IllegalArgumentException> { SServer(service, service) }.message
        )
        assertEquals(
            "no service id 987 found (method id 0)",
            assertFailsWith<IllegalStateException> {
                SServer(service).invocation(Request(987, 0, listOf()))
            }.message
        )
    }

    @Test
    fun proxy() = runBlocking {
        counter.set(0)
        assertEquals(4, divider.divide(12, 3))
        sAssertFailsWith<ArithmeticException> { divider.divide(12, 0) }
        divider.oneWay()
        assertEquals(5, counter.get())
        delay(100)
        assertEquals(6, counter.get())
    }

    @Test
    fun oneWayException() = runBlocking {
        divider.oneWayException()
    }
}
