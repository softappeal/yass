package ch.softappeal.yass.transport.ktor

import ch.softappeal.yass.remote.session.*
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.transport.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.*
import kotlinx.coroutines.sync.*
import kotlin.coroutines.*

class SSocketConnection(
    private val transport: SSessionTransport, private val aSocket: Socket, private val writeChannel: ByteWriteChannel
) : SConnection {
    private val writer = writeChannel.writer()

    val socket get() = aSocket

    private val writeMutex = Mutex()

    override suspend fun write(packet: Packet) = writeMutex.withLock {
        transport.write(writer, packet)
        writeChannel.flush()
    }

    override suspend fun closed() = aSocket.close()

    override fun launch(coroutineContext: CoroutineContext, block: suspend () -> Unit) {
        CoroutineScope(coroutineContext).launch { block() }
    }

    internal suspend fun create(reader: SReader) {
        val session = transport.sessionFactory()
        connection(session, this)
        try {
            session.opened()
            while (true) {
                val packet = transport.read(reader)
                received(session, packet)
                if (packet.isEnd) return
            }
        } catch (e: Exception) {
            throw throwClose(session, e)
        }
    }
}

fun CoroutineScope.sStartSocketInitiator(setup: SInitiatorSetup, socketConnector: SocketConnector): Job = launch {
    socketConnector().use { socket ->
        val writeChannel = socket.openWriteChannel()
        setup.writePath(writeChannel.writer())
        writeChannel.flush()
        SSocketConnection(setup.transport, socket, writeChannel).create(socket.openReadChannel().reader())
    }
}

fun CoroutineScope.sStartSocketAcceptor(
    acceptScope: CoroutineScope, serverSocket: ServerSocket, setup: SAcceptorSetup
): Job = launch {
    while (true) {
        val socket = serverSocket.accept()
        acceptScope.launch {
            socket.use {
                val reader = socket.openReadChannel().reader()
                SSocketConnection(setup.resolve(reader), socket, socket.openWriteChannel()).create(reader)
            }
        }
    }
}
