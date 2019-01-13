package ch.softappeal.yass.remote.session

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

interface Connection {
    /** Called if a packet has to be written out. */
    @Throws(Exception::class)
    fun write(packet: Packet)

    /** Called once if the connection has been closed. */
    @Throws(Exception::class)
    fun closed()
}

class SessionClosedException : RuntimeException()

abstract class Session : Client(), AutoCloseable {
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
        _connection = connection
        closed.set(false)
        dispatchOpened {
            try {
                opened()
            } catch (e: Exception) {
                close(e)
            }
        }
    }

    /** Called if a session has been opened. Must call [action] (possibly in an own thread). */
    @Throws(Exception::class)
    protected abstract fun dispatchOpened(action: () -> Unit)

    /** Called for an incoming request. Must call [action] (possibly in an own thread). */
    @Throws(Exception::class)
    protected abstract fun dispatchService(invocation: ServiceInvocation, action: () -> Unit)

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
        for (invocation in requestNumber2invocation.values.toList())
            try {
                invocation.settle(ExceptionReply(SessionClosedException()))
            } catch (ignore: Exception) {
            }
    }

    internal fun iClose(sendEnd: Boolean, exception: Exception?) {
        if (closed.getAndSet(true)) return
        try {
            try {
                settlePendingInvocations()
            } finally {
                closed(exception)
            }
            if (sendEnd) _connection.write(EndPacket)
        } finally {
            _connection.closed()
        }
    }

    private fun closeThrow(e: Exception) {
        addSuppressed(e) { close(e) }
        throw e
    }

    private fun serviceInvoke(requestNumber: Int, request: Request) {
        val invocation = server.invocation(true, request)
        dispatchService(invocation) {
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
        }
    }

    internal fun iReceived(packet: Packet) {
        try {
            if (packet.isEnd) {
                iClose(false, null)
                return
            }
            when (val message = packet.message) {
                is Request -> serviceInvoke(packet.requestNumber, message)
                is Reply -> requestNumber2invocation.remove(packet.requestNumber)!!.settle(message)
            }
        } catch (e: Exception) {
            closeThrow(e)
        }
    }

    final override fun invoke(invocation: ClientInvocation) {
        if (isClosed) throw SessionClosedException()
        try {
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
        } catch (e: Exception) {
            closeThrow(e)
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

typealias SessionFactory = () -> Session

open class SimpleSession(protected val dispatchExecutor: Executor) : Session() {
    override fun dispatchOpened(action: () -> Unit) = dispatchExecutor.execute { action() }
    override fun dispatchService(invocation: ServiceInvocation, action: () -> Unit) =
        dispatchExecutor.execute { action() }
}
