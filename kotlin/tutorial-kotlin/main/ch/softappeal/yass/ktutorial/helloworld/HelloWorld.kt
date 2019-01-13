package ch.softappeal.yass.ktutorial.helloworld

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.transport.*
import ch.softappeal.yass.transport.socket.*
import java.net.*
import java.util.concurrent.*

interface Calculator {
    fun add(a: Int, b: Int): Int
}

class CalculatorImpl : Calculator {
    override fun add(a: Int, b: Int) = a + b
}

fun useCalculator(calculator: Calculator) {
    println("2 + 3 = " + calculator.add(2, 3))
}

fun main() {
    val calculatorId = contractId<Calculator>(0, SimpleMethodMapperFactory)
    val messageSerializer = messageSerializer(JavaSerializer)
    val address = InetSocketAddress("localhost", 28947)
    val server = Server(
        Service(calculatorId, CalculatorImpl())
    )
    val executor = Executors.newCachedThreadPool(namedThreadFactory("executor", Terminate))
    try {
        socketServer(ServerSetup(server, messageSerializer), executor)
            .start(executor, socketBinder(address))
            .use {
                val client = socketClient(ClientSetup(messageSerializer), socketConnector(address))
                useCalculator(client.proxy(calculatorId))
            }
    } finally {
        executor.shutdown()
    }
}
