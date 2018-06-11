package ch.softappeal.yass.tutorial.py

import ch.softappeal.yass.remote.Client
import ch.softappeal.yass.transport.ClientSetup
import ch.softappeal.yass.transport.messageSerializer
import ch.softappeal.yass.transport.socket.socketClient
import ch.softappeal.yass.transport.socket.socketConnector
import ch.softappeal.yass.transport.socket.socketFactory
import ch.softappeal.yass.tutorial.contract.Expiration
import ch.softappeal.yass.tutorial.contract.Node
import ch.softappeal.yass.tutorial.contract.PriceKind
import ch.softappeal.yass.tutorial.contract.PyAcceptor
import ch.softappeal.yass.tutorial.contract.PyContractSerializer
import ch.softappeal.yass.tutorial.contract.SystemException
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock
import ch.softappeal.yass.tutorial.shared.Side
import ch.softappeal.yass.tutorial.shared.SslClient
import ch.softappeal.yass.tutorial.shared.logger
import ch.softappeal.yass.tutorial.shared.socket.Address

fun createObjects(): Any {
    val node1 = Node(1.0)
    val node2 = Node(2.0)
    node1.links.add(node1)
    node1.links.add(node2)
    return listOf(
        null,
        false,
        true,
        123456,
        -987654,
        1.34545e98,
        "Hello",
        ">\u0001\u0012\u007F\u0080\u0234\u07FF\u0800\u4321\uFFFF<",
        byteArrayOf(0, 127, -1, 10, -45),
        Expiration(2017, 11, 29),
        PriceKind.ASK,
        PriceKind.BID,
        Stock(123, "YASS", true),
        UnknownInstrumentsException(listOf(1, 2, 3)),
        node1
    )
}

val Serializer = PyContractSerializer

fun client(client: Client) {
    val echoService = client.proxy(PyAcceptor.echoService)
    val instrumentService = client.proxy(PyAcceptor.instrumentService, logger(null, Side.Client))
    println(echoService.echo("hello"))
    println(echoService.echo(createObjects()))
    try {
        echoService.echo("exception")
    } catch (e: SystemException) {
        println(e.details)
    }
    val big = ByteArray(1_000_000)
    if ((echoService.echo(big) as ByteArray).size != big.size) throw  RuntimeException()
    instrumentService.showOneWay(true, 123)
    println(instrumentService.instruments)
}

fun main(args: Array<String>) {
    client(socketClient(
        ClientSetup(messageSerializer(Serializer)),
        socketConnector(Address, SslClient.socketFactory)
    ))
}
