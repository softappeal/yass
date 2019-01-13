package ch.softappeal.yass.transport.ktor

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.transport.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

fun socketClient(setup: SClientSetup, socketConnector: suspend () -> Socket) = object : SClient() {
    override suspend fun invoke(request: Request, oneWay: Boolean): Reply? = socketConnector().use { socket ->
        val writeChannel = socket.openWriteChannel()
        setup.write(writeChannel.writer(), request)
        writeChannel.flush()
        if (oneWay) return null
        setup.read(socket.openReadChannel().reader())
    }
}

class SocketCCE(val socket: Socket) : AbstractCoroutineContextElement(SocketCCE) {
    companion object Key : CoroutineContext.Key<SocketCCE>
}

fun CoroutineScope.startSocketServer(
    acceptCoroutineScope: CoroutineScope, serverSocket: ServerSocket, setup: SServerSetup
): Job = launch {
    while (true) {
        val socket = serverSocket.accept()
        acceptCoroutineScope.launch(SocketCCE(socket)) {
            socket.use {
                val reader = socket.openReadChannel().reader()
                val transport = setup.resolve(reader)
                val invocation = transport.invocation(transport.read(reader))
                val reply = invocation.invoke()
                if (invocation.oneWay) return@use
                val writeChannel = socket.openWriteChannel()
                transport.write(writeChannel.writer(), reply)
                writeChannel.flush()
            }
        }
    }
}
