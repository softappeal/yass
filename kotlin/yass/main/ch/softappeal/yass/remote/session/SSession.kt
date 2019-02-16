package ch.softappeal.yass.remote.session

import ch.softappeal.yass.remote.*
import java.util.*
import java.util.Collections.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

interface SConnection {
    /** Must be thread-safe. */
    suspend fun write(packet: Packet)

    suspend fun closed()

    // needed because we don't want a dependency on kotlinx yet
    fun launch(coroutineContext: CoroutineContext, block: suspend () -> Unit)
}

abstract class SSession : SClient() {
    internal lateinit var _connection: SConnection
    val connection get() = _connection

    protected open fun server() = SServer()
    private val _server by lazy { server() }

    /** Must return immediately. */
    open fun opened() {}

    /** If ([exception] == null) regular close else reason for close. */
    protected open suspend fun closed(exception: Exception?) {}

    private val closed = AtomicBoolean(false)
    val isClosed get() = closed.get()

    suspend fun close() = close(true, null)

    /** Result must be thrown! */
    internal suspend fun throwClose(exception: Exception): Exception {
        try {
            close(false, exception)
        } finally {
            return exception
        }
    }

    private suspend fun close(sendEnd: Boolean, exception: Exception?) {
        if (closed.getAndSet(true)) return
        try {
            closed(exception)
            if (sendEnd) _connection.write(EndPacket)
        } finally {
            _connection.closed()
        }
    }

    private val nextRequestNumber = AtomicInteger(EndRequestNumber)
    private val requestNumber2continuation = synchronizedMap(HashMap<Int, Continuation<Reply>>(16))

    internal suspend fun received(packet: Packet) {
        if (packet.isEnd) {
            close(false, null)
            return
        }
        when (val message = packet.message) {
            is Request -> {
                val invocation = _server.invocation(message)
                val reply = invocation.invoke()
                if (!invocation.oneWay) _connection.write(Packet(packet.requestNumber, reply))
            }
            is Reply -> requestNumber2continuation.remove(packet.requestNumber)!!.resume(message)
            else -> error("unexpected type")
        }
    }

    final override suspend fun invoke(request: Request, oneWay: Boolean): Reply? {
        if (isClosed) throw SessionClosedException()
        try {
            var requestNumber: Int
            do requestNumber = nextRequestNumber.incrementAndGet() while (requestNumber == EndRequestNumber)
            return if (oneWay) {
                _connection.write(Packet(requestNumber, request))
                null
            } else {
                val coroutineContext = coroutineContext
                suspendCoroutineUninterceptedOrReturn { continuation ->
                    requestNumber2continuation[requestNumber] = continuation
                    _connection.launch(coroutineContext) {
                        try {
                            _connection.write(Packet(requestNumber, request))
                        } catch (e: Exception) {
                            throw throwClose(e)
                        }
                    }
                    COROUTINE_SUSPENDED
                }
            }
        } catch (e: Exception) {
            throw throwClose(e)
        }
    }
}

fun connection(session: SSession, connection: SConnection) {
    session._connection = connection
}

/** Result must be thrown! */
suspend fun throwClose(session: SSession, exception: Exception) = session.throwClose(exception)

suspend fun received(session: SSession, packet: Packet) = session.received(packet)

typealias SSessionFactory = () -> SSession
