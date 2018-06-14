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

    private lateinit var _connection: Connection
    val connection: Connection get() = _connection

    private lateinit var server: Server

    private val closed = AtomicBoolean(true)
    val isClosed get() = closed.get()

    /** note: it's not worth to use [ConcurrentHashMap] here */
    private val requestNumber2invocation = Collections.synchronizedMap(HashMap<Int, ClientInvocation>(16))
    private val nextRequestNumber = AtomicInteger(EndRequestNumber)

    internal fun iCreated(connection: Connection) {
        server = requireNotNull(server())
        closed.set(false)
        _connection = connection
        dispatchOpened(Runnable {
            try {
                opened = true
                opened()
            } catch (e: Exception) {
                close(e)
            }
        })
    }

    /** Called if a session has been opened. Must call [Runnable.run] (possibly in an own thread). */
    @Throws(Exception::class)
    protected abstract fun dispatchOpened(runnable: Runnable)

    /** Called for an incoming request. Must call [Runnable.run] (possibly in an own thread). */
    @Throws(Exception::class)
    protected abstract fun dispatchServerInvoke(invocation: ServerInvocation, runnable: Runnable)

    /** Gets the server of this session. Called only once after creation of session. */
    @Throws(Exception::class)
    protected open fun server(): Server = EmptyServer

    @Throws(Exception::class)
    protected open fun opened() {
        // empty
    }

    /** If ([exception] == null) regular close else reason for close. */
    @Throws(Exception::class)
    protected open fun closed(exception: Exception?) {
        // empty
    }

    override fun close() =
        iClose(true, null)

    private fun settlePendingInvocations() {
        for (invocation in ArrayList(requestNumber2invocation.values)) {
            try {
                invocation.settle(ExceptionReply(SessionClosedException()))
            } catch (ignore: Exception) {
            }
        }
    }

    internal fun iClose(sendEnd: Boolean, exception: Exception?) {
        if (closed.getAndSet(true)) return
        try {
            try {
                settlePendingInvocations()
            } finally {
                if (opened) closed(exception)
            }
            if (sendEnd) _connection.write(EndPacket)
        } finally {
            _connection.closed()
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
                            _connection.write(Packet(requestNumber, reply))
                        } catch (e: Exception) {
                            closeThrow(e)
                        }
                    }
                }
            } catch (e: Exception) {
                closeThrow(e)
            }
        })
    }

    internal fun iReceived(packet: Packet) {
        try {
            if (packet.isEnd) {
                iClose(false, null)
                return
            }
            val message = packet.message
            when (message) {
                is Request -> serverInvoke(packet.requestNumber, message)
                is Reply -> requestNumber2invocation.remove(packet.requestNumber)!!.settle(message)
            }
        } catch (e: Exception) {
            closeThrow(e)
        }
    }

    final override fun invoke(invocation: ClientInvocation) {
        if (isClosed) throw SessionClosedException()
        invocation.invoke(true) { request ->
            try {
                var requestNumber: Int
                do { // we can't use END_REQUEST_NUMBER as regular requestNumber
                    requestNumber = nextRequestNumber.incrementAndGet()
                } while (requestNumber == EndRequestNumber)
                if (!invocation.methodMapping.oneWay) {
                    requestNumber2invocation[requestNumber] = invocation
                    if (isClosed) settlePendingInvocations() // needed due to race conditions
                }
                _connection.write(Packet(requestNumber, request))
            } catch (e: Exception) {
                closeThrow(e)
            }
        }
    }
}

/** Must be called if communication has failed. This method is idempotent. */
fun Session.close(e: Exception) = iClose(false, e)

/**
 * Must be called if a packet has been received.
 * It must also be called if [Packet.isEnd]; however, it must not be called again after that.
 */
fun Session.received(packet: Packet) = iReceived(packet)

fun Session.created(connection: Connection) = iCreated(connection)

open class SimpleSession(protected val dispatchExecutor: Executor) : Session() {
    override fun dispatchOpened(runnable: Runnable) = dispatchExecutor.execute(runnable)
    override fun dispatchServerInvoke(invocation: ServerInvocation, runnable: Runnable) = dispatchExecutor.execute(runnable)
}
