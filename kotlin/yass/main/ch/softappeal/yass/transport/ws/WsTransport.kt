@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.transport.ws

import ch.softappeal.yass.remote.session.Connection
import ch.softappeal.yass.remote.session.Packet
import ch.softappeal.yass.remote.session.Session
import ch.softappeal.yass.remote.session.close
import ch.softappeal.yass.remote.session.created
import ch.softappeal.yass.remote.session.received
import ch.softappeal.yass.serialize.ByteBufferOutputStream
import ch.softappeal.yass.serialize.reader
import ch.softappeal.yass.serialize.writer
import ch.softappeal.yass.transport.SessionTransport
import java.nio.ByteBuffer
import java.util.stream.Collectors
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.Extension
import javax.websocket.HandshakeResponse
import javax.websocket.MessageHandler
import javax.websocket.RemoteEndpoint
import javax.websocket.server.HandshakeRequest
import javax.websocket.server.ServerEndpointConfig

abstract class WsConnection protected constructor(private val configurator: WsConfigurator, val session: javax.websocket.Session) : Connection {
    internal lateinit var yassSession: Session

    internal fun writeToBuffer(packet: Packet): ByteBuffer {
        val buffer = ByteBufferOutputStream(128)
        configurator.transport.write(writer(buffer), packet)
        return buffer.toByteBuffer()
    }

    internal fun onClose(closeReason: CloseReason) {
        if (closeReason.closeCode.code == CloseReason.CloseCodes.NORMAL_CLOSURE.code)
            yassSession.close()
        else
            onError(RuntimeException(closeReason.toString()))
    }

    fun onError(t: Throwable?) = when (t) {
        null -> yassSession.close(Exception())
        is Exception -> yassSession.close(t)
        else -> configurator.uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t)
    }

    override fun closed() = session.close()
}

typealias WsConnectionFactory = (configurator: WsConfigurator, session: javax.websocket.Session) -> WsConnection

/** Sends messages synchronously. Blocks if socket can't send data. */
val SyncWsConnectionFactory: WsConnectionFactory = { configurator, session ->
    object : WsConnection(configurator, session) {
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
fun AsyncWsConnectionFactory(sendTimeoutMilliSeconds: Long): WsConnectionFactory = { configurator, session ->
    require(sendTimeoutMilliSeconds >= 0) { "sendTimeoutMilliSeconds < 0" }
    object : WsConnection(configurator, session) {
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

private fun create(configurator: WsConfigurator, session: javax.websocket.Session): WsConnection {
    val yassSession = configurator.transport.session()
    try {
        val connection = configurator.connectionFactory(configurator, session)
        yassSession.created(connection)
        connection.yassSession = yassSession
        session.addMessageHandler(MessageHandler.Whole<ByteBuffer> { `in` ->
            // note: could be replaced with a lambda in WebSocket API 1.1 but we would loose compatibility with 1.0
            try {
                connection.yassSession.received(configurator.transport.read(reader(`in`)))
                if (`in`.hasRemaining()) {
                    throw RuntimeException("input buffer is not empty")
                }
            } catch (e: Exception) {
                connection.yassSession.close(e)
            }
        })
        return connection
    } catch (e: Exception) {
        try {
            session.close()
        } catch (e2: Exception) {
            e.addSuppressed(e2)
        }
        throw e
    }
}

class WsConfigurator(
    internal val connectionFactory: WsConnectionFactory, internal val transport: SessionTransport, internal val uncaughtExceptionHandler: Thread.UncaughtExceptionHandler
) : ServerEndpointConfig.Configurator() {
    val endpointInstance: Endpoint = getEndpointInstance(Endpoint::class.java)

    override fun <T> getEndpointInstance(endpointClass: Class<T>): T = endpointClass.cast(object : Endpoint() {
        @Volatile
        private var connection: WsConnection? = null

        override fun onOpen(session: javax.websocket.Session, config: EndpointConfig) {
            try {
                connection = create(this@WsConfigurator, session)
            } catch (t: Throwable) {
                uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t)
            }
        }

        override fun onClose(session: javax.websocket.Session?, closeReason: CloseReason?) {
            if (connection != null) connection!!.onClose(closeReason!!)
        }

        override fun onError(session: javax.websocket.Session?, throwable: Throwable?) {
            if (connection != null) connection!!.onError(throwable)
        }
    })

    override fun getNegotiatedSubprotocol(supported: List<String>, requested: List<String>): String =
        requested.stream().filter({ supported.contains(it) }).findFirst().orElse("")

    override fun getNegotiatedExtensions(installed: List<Extension>, requested: List<Extension>): List<Extension> =
        requested.stream().filter { r -> installed.stream().anyMatch { i -> i.name == r.name } }.collect(Collectors.toList())

    override fun checkOrigin(originHeaderValue: String?) = true
    override fun modifyHandshake(sec: ServerEndpointConfig?, request: HandshakeRequest?, response: HandshakeResponse?) {}
}
