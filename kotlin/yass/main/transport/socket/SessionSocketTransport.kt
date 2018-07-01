package ch.softappeal.yass.transport.socket

import ch.softappeal.yass.remote.session.Connection
import ch.softappeal.yass.remote.session.Packet
import ch.softappeal.yass.remote.session.Session
import ch.softappeal.yass.remote.session.close
import ch.softappeal.yass.remote.session.created
import ch.softappeal.yass.remote.session.received
import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.reader
import ch.softappeal.yass.serialize.writer
import ch.softappeal.yass.transport.AcceptorSetup
import ch.softappeal.yass.transport.InitiatorSetup
import ch.softappeal.yass.transport.SessionTransport
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

abstract class SocketConnection internal constructor(
    private val transport: SessionTransport, val socket: Socket, private val out: OutputStream
) : Connection {
    internal fun writeToBuffer(packet: Packet): ByteArrayOutputStream {
        val buffer = ByteArrayOutputStream(128)
        transport.write(writer(buffer), packet)
        return buffer
    }

    /** Buffering of output is needed to prevent long delays due to Nagle's algorithm. */
    internal fun flush(buffer: ByteArrayOutputStream) {
        buffer.writeTo(out)
        out.flush()
    }

    override fun closed() =
        socket.close()
}

typealias SocketConnectionFactory =
    (session: Session, transport: SessionTransport, socket: Socket, out: OutputStream) -> SocketConnection

/** Writes to socket in caller thread. Blocks if socket can't send data. */
val SyncSocketConnectionFactory: SocketConnectionFactory = { _, transport, socket, out ->
    object : SocketConnection(transport, socket, out) {
        val writeMutex = Any()
        override fun write(packet: Packet) {
            val buffer = writeToBuffer(packet)
            synchronized(writeMutex) { flush(buffer) }
        }
    }
}

/** Writes to socket in a writer thread. Caller thread never blocks. [writerExecutor] is used once for each session. */
fun asyncSocketConnectionFactory(writerExecutor: Executor, writerQueueSize: Int): SocketConnectionFactory {
    require(writerQueueSize >= 1) { "writerQueueSize < 1" }
    return { session, transport, socket, out ->
        object : SocketConnection(transport, socket, out) {
            @Volatile
            var closed = false
            val writerQueue: BlockingQueue<ByteArrayOutputStream> = ArrayBlockingQueue(writerQueueSize)

            init {
                writerExecutor.execute {
                    try {
                        while (true) {
                            val buffer = writerQueue.poll(1L, TimeUnit.SECONDS)
                            if (buffer == null) {
                                if (closed) break
                                continue
                            }
                            while (true) { // drain queue -> batching of packets
                                val buffer2 = writerQueue.poll() ?: break
                                buffer2.writeTo(buffer)
                            }
                            flush(buffer)
                        }
                    } catch (e: Exception) {
                        session.close(e)
                    }
                }
            }

            override fun write(packet: Packet) =
                check(writerQueue.offer(writeToBuffer(packet))) { "writer queue full" }

            override fun closed() {
                try {
                    TimeUnit.SECONDS.sleep(1L) // give the socket a chance to write the end packet
                } finally {
                    closed = true // terminates writer thread
                    super.closed()
                }
            }
        }
    }
}

private fun read(
    connectionFactory: SocketConnectionFactory, transport: SessionTransport, socket: Socket, reader: Reader, out: OutputStream
) {
    val session = transport.sessionFactory()
    try {
        session.created(connectionFactory(session, transport, socket, out))
        while (true) {
            val packet = transport.read(reader)
            session.received(packet)
            if (packet.isEnd) return
        }
    } catch (e: Exception) {
        session.close(e)
    }
}

/** [readerExecutor] is called once for each session. */
fun socketAcceptor(setup: AcceptorSetup, readerExecutor: Executor, connectionFactory: SocketConnectionFactory) =
    object : SocketListener(readerExecutor) {
        override fun accept(socket: Socket) {
            val reader = reader(socket.getInputStream())
            read(connectionFactory, setup.resolve(reader), socket, reader, socket.getOutputStream())
        }
    }

/** [readerExecutor] is called once. */
fun socketInitiator(
    setup: InitiatorSetup, readerExecutor: Executor, connectionFactory: SocketConnectionFactory, socketConnector: SocketConnector
) = execute(readerExecutor, socketConnector()) { socket ->
    val out = socket.getOutputStream()
    setup.writePath(writer(out))
    out.flush()
    read(connectionFactory, setup.transport, socket, reader(socket.getInputStream()), out)
}
