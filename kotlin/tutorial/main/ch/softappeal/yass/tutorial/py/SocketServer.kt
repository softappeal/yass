package ch.softappeal.yass.tutorial.py

import ch.softappeal.yass.Interceptor
import ch.softappeal.yass.StdErr
import ch.softappeal.yass.namedThreadFactory
import ch.softappeal.yass.remote.Server
import ch.softappeal.yass.remote.Service
import ch.softappeal.yass.transport.ServerSetup
import ch.softappeal.yass.transport.messageSerializer
import ch.softappeal.yass.transport.socket.serverSocketFactory
import ch.softappeal.yass.transport.socket.socket
import ch.softappeal.yass.transport.socket.socketBinder
import ch.softappeal.yass.transport.socket.socketServer
import ch.softappeal.yass.tutorial.contract.EchoService
import ch.softappeal.yass.tutorial.contract.Instrument
import ch.softappeal.yass.tutorial.contract.PyAcceptor
import ch.softappeal.yass.tutorial.contract.SystemException
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService
import ch.softappeal.yass.tutorial.shared.Side
import ch.softappeal.yass.tutorial.shared.SslServer
import ch.softappeal.yass.tutorial.shared.logger
import ch.softappeal.yass.tutorial.shared.socket.Address
import java.util.concurrent.Executors
import javax.net.ssl.SSLSocket

private val EchoServiceImpl = object : EchoService {
    override fun echo(value: Any?): Any? {
        if ("exception" == value) throw  SystemException("exception")
        return value
    }
}

private val InstrumentServiceImpl = object : InstrumentService {
    override val instruments: List<Instrument> get() = emptyList()
    override fun showOneWay(testBoolean: Boolean, testInt: Int) {}
}

private val Peer: Interceptor = { _, _, invocation ->
    println((socket as SSLSocket).session.peerPrincipal.name)
    invocation()
}

fun main(args: Array<String>) {
    val executor = Executors.newCachedThreadPool(namedThreadFactory("executor", StdErr))
    socketServer(
        ServerSetup(
            Server(
                Service(PyAcceptor.echoService, EchoServiceImpl),
                Service(PyAcceptor.instrumentService, InstrumentServiceImpl, Peer, logger(null, Side.Server))
            ),
            messageSerializer(Serializer)
        ),
        executor
    ).start(
        executor,
        socketBinder(Address, SslServer.serverSocketFactory)
    )
    println("started")
}
