@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.transport.socket

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.util.concurrent.Executor
import javax.net.ServerSocketFactory
import javax.net.SocketFactory

internal fun close(socket: Socket, e: Exception) = try {
    socket.close()
} catch (e2: Exception) {
    e.addSuppressed(e2)
}

internal fun close(serverSocket: ServerSocket, e: Exception) = try {
    serverSocket.close()
} catch (e2: Exception) {
    e.addSuppressed(e2)
}

typealias SocketConnector = () -> Socket

@JvmOverloads
fun socketConnector(
    socketAddress: SocketAddress, socketFactory: SocketFactory = SocketFactory.getDefault(),
    connectTimeoutMilliSeconds: Int = 0, readTimeoutMilliSeconds: Int = 0
): SocketConnector {
    require(connectTimeoutMilliSeconds >= 0)
    require(readTimeoutMilliSeconds >= 0)
    return {
        val socket = socketFactory.createSocket()
        try {
            socket.connect(socketAddress, connectTimeoutMilliSeconds)
            socket.soTimeout = readTimeoutMilliSeconds
            socket
        } catch (e: Exception) {
            close(socket, e)
            throw e
        }
    }
}

@SafeVarargs
fun firstSocketConnector(vararg socketConnectors: SocketConnector): SocketConnector = fun(): Socket {
    for (socketConnector in socketConnectors)
        try {
            return socketConnector()
        } catch (ignore: Exception) {
        }
    error("all connectors failed")
}

typealias SocketBinder = () -> ServerSocket

@JvmOverloads
fun socketBinder(socketAddress: SocketAddress, socketFactory: ServerSocketFactory = ServerSocketFactory.getDefault()): SocketBinder = {
    val serverSocket = socketFactory.createServerSocket()
    try {
        serverSocket.bind(socketAddress)
        serverSocket
    } catch (e: Exception) {
        close(serverSocket, e)
        throw e
    }
}

internal fun setForceImmediateSend(socket: Socket) {
    socket.tcpNoDelay = true
}

internal typealias SocketExecutor = (socket: Socket) -> Unit

internal fun execute(executor: Executor, socket: Socket, socketExecutor: SocketExecutor): Unit = try {
    executor.execute {
        try {
            setForceImmediateSend(socket)
            socketExecutor(socket)
        } catch (e: Exception) {
            close(socket, e)
            throw e
        }
    }
} catch (e: Exception) {
    close(socket, e)
    throw e
}

/** acceptExecutor is called once for each accept call. */
abstract class SocketListener internal constructor(private val acceptExecutor: Executor) {
    internal abstract fun accept(socket: Socket)
    /** Starts a socket listener that can be closed with the result. [listenerExecutor] is called once. */
    fun start(listenerExecutor: Executor, socketBinder: SocketBinder): AutoCloseable {
        val serverSocket = socketBinder()
        try {
            listenerExecutor.execute {
                try {
                    while (true) execute(acceptExecutor, serverSocket.accept(), ::accept)
                } catch (e: Exception) {
                    if (serverSocket.isClosed) return@execute
                    close(serverSocket, e)
                    throw e
                }
            }
        } catch (e: Exception) {
            close(serverSocket, e)
            throw e
        }
        return AutoCloseable { serverSocket.close() }
    }
}

private fun unsupported(): Nothing = throw UnsupportedOperationException()

abstract class AbstractSocketFactory : SocketFactory() {
    override fun createSocket(host: String, port: Int): Socket = unsupported()
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket = unsupported()
    override fun createSocket(host: InetAddress, port: Int): Socket = unsupported()
    override fun createSocket(host: InetAddress, port: Int, localHost: InetAddress, localPort: Int): Socket = unsupported()
}

abstract class AbstractServerSocketFactory : ServerSocketFactory() {
    override fun createServerSocket(port: Int): ServerSocket = unsupported()
    override fun createServerSocket(port: Int, backlog: Int): ServerSocket = unsupported()
    override fun createServerSocket(port: Int, backlog: Int, address: InetAddress): ServerSocket = unsupported()
}
