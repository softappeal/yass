package ch.softappeal.yass.tutorial

import ch.softappeal.yass.remote.Server
import ch.softappeal.yass.remote.Service
import ch.softappeal.yass.remote.SimpleMethodMapperFactory
import ch.softappeal.yass.remote.contractId
import ch.softappeal.yass.serialize.JavaSerializer
import ch.softappeal.yass.transport.ClientSetup
import ch.softappeal.yass.transport.ServerSetup
import ch.softappeal.yass.transport.messageSerializer
import ch.softappeal.yass.transport.socket.socketBinder
import ch.softappeal.yass.transport.socket.socketClient
import ch.softappeal.yass.transport.socket.socketConnector
import ch.softappeal.yass.transport.socket.socketServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors

interface Calculator {
    fun add(a: Int, b: Int): Int
}

class CalculatorImpl : Calculator {
    override fun add(a: Int, b: Int) = a + b
}

fun useCalculator(calculator: Calculator) {
    println("2 + 3 = " + calculator.add(2, 3))
}

fun main(args: Array<String>) {
    val calculatorId = contractId<Calculator>(0, SimpleMethodMapperFactory)
    val messageSerializer = messageSerializer(JavaSerializer)
    val address = InetSocketAddress("localhost", 28947)
    val executor = Executors.newCachedThreadPool()
    val server = Server(Service(calculatorId, CalculatorImpl()))
    socketServer(ServerSetup(server, messageSerializer), executor)
        .start(executor, socketBinder(address))
    val client = socketClient(ClientSetup(messageSerializer), socketConnector(address))
    useCalculator(client.proxy(calculatorId))
}
