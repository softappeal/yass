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
        val transport: ServerTransport
        val invocation: ServerInvocation
        try {
            val reader = reader(socket.getInputStream())
            transport = setup.resolve(reader)
            invocation = transport.invocation(true, transport.read(reader))
        } catch (e: Exception) {
            close(socket, e)
            throw e
        }
        threadLocal(socket_, socket) {
            invocation.invoke({ socket.close() }) { reply ->
                val buffer = createBuffer()
                transport.write(writer(buffer), reply)
                write(buffer, socket)
            }
        }
    }
}

fun socketClient(setup: ClientSetup, socketConnector: SocketConnector) = object : Client() {
    override fun executeInContext(action: () -> Any?) = socketConnector().use { socket ->
        setForceImmediateSend(socket)
        threadLocal(socket_, socket) { action() }
    }

    override fun invoke(invocation: ClientInvocation) = invocation.invoke(false) { request ->
        val buffer = createBuffer()
        setup.write(writer(buffer), request)
        val socket = socket_.get()
        write(buffer, socket)
        if (!invocation.methodMapping.oneWay) {
            invocation.settle(setup.read(reader(socket.getInputStream())))
        }
    }
}
