package ch.softappeal.yass.transport.ws

import ch.softappeal.yass.remote.session.*
import ch.softappeal.yass.remote.session.Session
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.transport.*
import java.nio.*
import javax.websocket.*
import javax.websocket.server.*

abstract class WsConnection internal constructor(
    private val transport: SessionTransport, val session: javax.websocket.Session
) : Connection {
    internal lateinit var yassSession: Session

    internal fun writeToBuffer(packet: Packet): ByteBuffer {
        val buffer = ByteBufferOutputStream(128)
        transport.write(writer(buffer), packet)
        return buffer.toByteBuffer()
    }

    internal fun onClose(closeReason: CloseReason) {
        if (closeReason.closeCode.code == CloseReason.CloseCodes.NORMAL_CLOSURE.code)
            yassSession.close()
        else
            onError(RuntimeException(closeReason.toString()))
    }

    internal fun onError(t: Throwable?) = when (t) {
        null -> yassSession.close(Exception())
        is Exception -> yassSession.close(t)
        else -> throw t
    }

    override fun closed() =
        session.close()
}

typealias WsConnectionFactory = (transport: SessionTransport, session: javax.websocket.Session) -> WsConnection

/** Sends messages synchronously. Blocks if socket can't send data. */
val SyncWsConnectionFactory: WsConnectionFactory = { transport, session ->
    object : WsConnection(transport, session) {
        private val writeMutex = Any()
        override fun write(packet: Packet) {
            val buffer = writeToBuffer(packet)
            synchronized(writeMutex) {
                session.basicRemote.sendBinary(buffer)
            }
        }
    }
}

/** Sends messages asynchronously. Closes session if timeout reached. */
fun asyncWsConnectionFactory(sendTimeoutMilliSeconds: Long): WsConnectionFactory = { transport, session ->
    require(sendTimeoutMilliSeconds >= 0) { "sendTimeoutMilliSeconds < 0" }
    object : WsConnection(transport, session) {
        private val remoteEndpoint: RemoteEndpoint.Async = session.asyncRemote

        init {
            remoteEndpoint.sendTimeout = sendTimeoutMilliSeconds
        }

        override fun write(packet: Packet) = remoteEndpoint.sendBinary(writeToBuffer(packet)) { result ->
            if (result == null) {
                onError(null)
            } else if (!result.isOK) {
                onError(result.exception)
            }
        }
    }
}

open class WsConfigurator(
    private val connectionFactory: WsConnectionFactory, private val transport: SessionTransport
) : ServerEndpointConfig.Configurator() {
    val endpointInstance: Endpoint = getEndpointInstance(Endpoint::class.java)

    @Suppress("UNCHECKED_CAST")
    final override fun <T> getEndpointInstance(endpointClass: Class<T>): T = object : Endpoint() {
        @Volatile
        private var connection: WsConnection? = null

        override fun onOpen(session: javax.websocket.Session, config: EndpointConfig) {
            try {
                connection = connectionFactory(transport, session)
                connection!!.yassSession = transport.sessionFactory()
                connection!!.yassSession.created(connection!!)
                session.addMessageHandler(MessageHandler.Whole<ByteBuffer> { input ->
                    // note: could be replaced with a lambda in WebSocket API 1.1
                    //       but we would loose compatibility with 1.0
                    try {
                        connection!!.yassSession.received(transport.read(reader(input)))
                        check(!input.hasRemaining()) { "input buffer is not empty" }
                    } catch (e: Exception) {
                        connection!!.yassSession.close(e)
                    }
                })
            } catch (e: Exception) {
                try {
                    session.close()
                } catch (e2: Exception) {
                    e.addSuppressed(e2)
                }
                throw e
            }
        }

        override fun onClose(session: javax.websocket.Session?, closeReason: CloseReason?) {
            if (connection != null) connection!!.onClose(closeReason!!)
        }

        override fun onError(session: javax.websocket.Session?, throwable: Throwable?) {
            if (connection != null) connection!!.onError(throwable)
        }
    } as T

    override fun getNegotiatedSubprotocol(supported: List<String>, requested: List<String>): String =
        requested.firstOrNull { supported.contains(it) } ?: ""

    override fun getNegotiatedExtensions(installed: List<Extension>, requested: List<Extension>): List<Extension> =
        requested.filter { r -> installed.any { i -> i.name == r.name } }

    override fun checkOrigin(originHeaderValue: String?) =
        true
}
