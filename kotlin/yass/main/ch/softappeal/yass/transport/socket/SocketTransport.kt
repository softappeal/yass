package ch.softappeal.yass.transport.socket

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.transport.*
import java.io.*
import java.net.*
import java.util.concurrent.*

private val socket_ = ThreadLocal<Socket>()

val socket: Socket
    get() = checkNotNull(socket_.get()) { "no active invocation" }

/** Buffering of output is needed to prevent long delays due to Nagle's algorithm. */
private fun createBuffer(): ByteArrayOutputStream = ByteArrayOutputStream(128)

private fun write(buffer: ByteArrayOutputStream, socket: Socket) {
    val out = socket.getOutputStream()
    buffer.writeTo(out)
    out.flush()
}

/** [requestExecutor] is called once for each request. */
fun socketServer(setup: ServerSetup, requestExecutor: Executor) = object : SocketListener(requestExecutor) {
    override fun accept(socket: Socket) {
        val reader = reader(socket.getInputStream())
        val transport = setup.resolve(reader)
        threadLocal(socket_, socket) {
            transport.invocation(true, transport.read(reader)).invoke({ socket.close() }) { reply ->
                val buffer = createBuffer()
                transport.write(writer(buffer), reply)
                write(buffer, socket)
            }
        }
    }
}

fun socketClient(setup: ClientSetup, socketConnector: SocketConnector) = object : Client() {
    override fun executeInContext(action: () -> Any?): Any? {
        val socket = socketConnector()
        try {
            setForceImmediateSend(socket)
        } catch (e: Exception) {
            close(socket, e)
            throw e
        }
        return threadLocal(socket_, socket) { action() }
    }

    override fun invoke(invocation: ClientInvocation) = socket_.get().use { socket ->
        invocation.invoke(true) { request ->
            val buffer = createBuffer()
            setup.write(writer(buffer), request)
            write(buffer, socket)
            if (!invocation.methodMapping.oneWay) {
                invocation.settle(setup.read(reader(socket.getInputStream())))
            }
        }
    }
}
