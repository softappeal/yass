package ch.softappeal.yass.transport.socket

import ch.softappeal.yass.Interceptor
import ch.softappeal.yass.remote.Client
import ch.softappeal.yass.remote.ClientInvocation
import ch.softappeal.yass.remote.ContractId
import ch.softappeal.yass.serialize.reader
import ch.softappeal.yass.serialize.writer
import ch.softappeal.yass.transport.ClientSetup
import ch.softappeal.yass.transport.ServerSetup
import java.io.ByteArrayOutputStream
import java.lang.reflect.Method
import java.net.Socket
import java.util.concurrent.Executor

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
    override fun accept(socket: Socket) = socket.use {
        val oldSocket = socket_.get()
        socket_.set(socket)
        try {
            val reader = reader(socket.getInputStream())
            val transport = setup.resolve(reader)
            transport.invocation(false, transport.read(reader)).invoke { reply ->
                val buffer = createBuffer()
                transport.write(writer(buffer), reply)
                write(buffer, socket)
            }
        } finally {
            socket_.set(oldSocket)
        }
    }
}

fun socketClient(setup: ClientSetup, socketConnector: SocketConnector) = object : Client() {
    override fun syncInvoke(contractId: ContractId<*>, interceptor: Interceptor, method: Method, arguments: List<Any?>): Any? {
        socketConnector().use { socket ->
            setForceImmediateSend(socket)
            val oldSocket = socket_.get()
            socket_.set(socket)
            try {
                return super.syncInvoke(contractId, interceptor, method, arguments)
            } finally {
                socket_.set(oldSocket)
            }
        }
    }

    override fun invoke(invocation: ClientInvocation) = invocation.invoke(false) { request ->
        val buffer = createBuffer()
        setup.write(writer(buffer), request)
        val socket = socket_.get()
        write(buffer, socket)
        invocation.settle(setup.read(reader(socket.getInputStream())))
    }
}
