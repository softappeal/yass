@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.remote.session

import ch.softappeal.yass.remote.Client
import ch.softappeal.yass.remote.ClientInvocation
import ch.softappeal.yass.remote.EmptyServer
import ch.softappeal.yass.remote.ExceptionReply
import ch.softappeal.yass.remote.Reply
import ch.softappeal.yass.remote.Request
import ch.softappeal.yass.remote.Server
import ch.softappeal.yass.remote.ServerInvocation
import java.util.Collections
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

interface Connection {
    /** Called if a packet has to be written out. */
    fun write(packet: Packet)

    /** Called once if the connection has been closed. */
    fun closed()
}

class SessionClosedException : RuntimeException()

abstract class Session : Client(), AutoCloseable {
    @Volatile
    private var opened = false
    private lateinit var connection: Connection
    fun connection(): Connection = connection
    private lateinit var server: Server
    private val closed = AtomicBoolean(true)
    fun isClosed() = closed.get()
    /** note: it's not worth to use [ConcurrentHashMap] here */
    private val requestNumber2invocation = Collections.synchronizedMap(HashMap<Int, ClientInvocation>(16))
    private val nextRequestNumber = AtomicInteger(EndRequestNumber)
    /** Called if a session has been opened. Must call [Runnable.run] (possibly in an own thread). */
    protected abstract fun dispatchOpened(runnable: Runnable)

    /** Called for an incoming request. Must call [Runnable.run] (possibly in an own thread). */
    protected abstract fun dispatchServerInvoke(invocation: ServerInvocation, runnable: Runnable)

    /** Gets the server of this session. Called only once after creation of session. */
    protected open fun server(): Server = EmptyServer

    @Throws(Exception::class)
    protected open fun opened() {
    }

    /** if ([exception] == null) regular close else reason for close */
    @Throws(Exception::class)
    protected open fun closed(exception: Exception?) {
    }

    override fun close() = close(true, null)
    private fun settlePendingInvocations() {
        for (invocation in ArrayList(requestNumber2invocation.values)) {
            try {
                invocation.settle(ExceptionReply(SessionClosedException()))
            } catch (ignore: Exception) {
            }
        }
    }

    internal fun close(sendEnd: Boolean, exception: Exception?) {
        if (closed.getAndSet(true)) return
        try {
            try {
                settlePendingInvocations()
            } finally {
                if (opened) closed(exception)
            }
            if (sendEnd) connection.write(EndPacket)
        } finally {
            connection.closed()
        }
    }

    private fun closeThrow(e: Exception) {
        try {
            close(e)
        } catch (e2: Exception) {
            e.addSuppressed(e2)
        }
        throw e
    }

    private fun serverInvoke(requestNumber: Int, request: Request) {
        val invocation = server.invocation(true, request)
        dispatchServerInvoke(invocation, Runnable {
            try {
                invocation.invoke { reply ->
                    if (!invocation.methodMapping.oneWay) {
                        try {
                            connection.write(Packet(requestNumber, reply))
                        } catch (e: Exception) {
                            closeThrow(e)
                        }
                    }
                }
            } catch (e: Exception) {
                close(e)
            }
        })
    }

    internal fun received2(packet: Packet) {
        try {
            if (packet.isEnd()) {
                close(false, null)
                return
            }
            val message = packet.message()
            when (message) {
                is Request -> serverInvoke(packet.requestNumber(), message)
                is Reply -> requestNumber2invocation.remove(packet.requestNumber())!!.settle(message)
            }
        } catch (e: Exception) {
            closeThrow(e)
        }
    }

    final override fun invoke(invocation: ClientInvocation) {
        if (isClosed()) throw SessionClosedException()
        invocation.invoke(true, { request ->
            try {
                var requestNumber: Int
                do { // we can't use END_REQUEST_NUMBER as regular requestNumber
                    requestNumber = nextRequestNumber.incrementAndGet()
                } while (requestNumber == EndRequestNumber)
                if (!invocation.methodMapping.oneWay) {
                    requestNumber2invocation[requestNumber] = invocation
                    if (isClosed()) settlePendingInvocations() // needed due to race conditions
                }
                connection.write(Packet(requestNumber, request))
            } catch (e: Exception) {
                closeThrow(e)
            }
        })
    }

    internal fun created2(connection: Connection) {
        server = requireNotNull(server())
        closed.set(false)
        this.connection = connection
        dispatchOpened(Runnable {
            try {
                opened = true
                opened()
            } catch (e: Exception) {
                close(e)
            }
        })
    }
}

/** Must be called if communication has failed. This method is idempotent. */
fun Session.close(e: Exception) = close(false, e)

/**
 * Must be called if a packet has been received.
 * It must also be called if [Packet.isEnd]; however, it must not be called again after that.
 */
fun Session.received(packet: Packet) = received2(packet)

fun Session.created(connection: Connection) = created2(connection)

abstract class SimpleSession protected constructor(protected val dispatchExecutor: Executor) : Session() {
    final override fun dispatchOpened(runnable: Runnable) = dispatchExecutor.execute(runnable)
    final override fun dispatchServerInvoke(invocation: ServerInvocation, runnable: Runnable) = dispatchExecutor.execute(runnable)
}
