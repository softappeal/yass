package ch.softappeal.yass.ktutorial.session

import ch.softappeal.yass.*
import ch.softappeal.yass.remote.*
import ch.softappeal.yass.remote.session.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.*

fun threadPrintln(s: String) {
    println("${Date()} | ${Thread.currentThread().name} | $s")
}

private val EchoServiceImpl = object : EchoService {
    override suspend fun echo(value: Any?): Any? = value
}

private val CalculatorImpl = object : Calculator {
    override suspend fun add(a: Int, b: Int): Int {
        return a + b
    }

    override suspend fun divide(a: Int, b: Int): Int {
        if (b == 0) throw DivisionByZeroException()
        return a / b
    }
}

private val WeatherListenerImpl = object : WeatherListener {
    override suspend fun update(weather: Weather) {
        threadPrintln("weather update: $weather")
    }
}

private fun printer(side: String): SInterceptor = { method, arguments, invocation ->
    fun print(type: String, data: Any?, arguments: List<Any?>? = null) =
        threadPrintln("$side - $type - $data ${arguments ?: ""}")
    print("enter", method.name, arguments)
    try {
        val result = invocation()
        print("exit", result)
        result
    } catch (e: Exception) {
        print("exception", e)
        throw e
    }
}

private val ClientPrinter = printer("client")
private val ServerPrinter = printer("server")

abstract class BaseSession : SSession() {
    protected abstract fun connectionContext(): String

    override suspend fun closed(exception: Exception?) {
        threadPrintln("$this closed $exception")
    }
}

val TutorialDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

abstract class InitiatorSession : BaseSession() {
    override fun server() = SServer(
        SService(Initiator.echoService, EchoServiceImpl, ServerPrinter),
        SService(Initiator.weatherListener, WeatherListenerImpl, ServerPrinter)
    )

    private val echoService = proxy(Acceptor.echoService, ClientPrinter)
    private val calculator = proxy(Acceptor.calculator, ClientPrinter)

    override fun opened() {
        threadPrintln("$this ${connectionContext()} opened")
        CoroutineScope(TutorialDispatcher).launch {
            threadPrintln("${echoService.echo("hello from initiator")}")
            threadPrintln("${calculator.add(1, 2)}")
            threadPrintln("${calculator.divide(12, 4)}")
            try {
                calculator.divide(12, 0)
            } catch (e: DivisionByZeroException) {
                threadPrintln("$e")
            }
        }
    }
}

abstract class AcceptorSession : BaseSession() {
    override fun server() = SServer(
        SService(Acceptor.echoService, EchoServiceImpl, ServerPrinter),
        SService(Acceptor.calculator, CalculatorImpl, ServerPrinter)
    )

    private val echoService = proxy(Initiator.echoService, ClientPrinter)
    private val weatherListener = proxy(Initiator.weatherListener, ClientPrinter)

    override fun opened() {
        threadPrintln("$this ${connectionContext()} opened")
        CoroutineScope(TutorialDispatcher).launch {
            threadPrintln("${echoService.echo("hello from acceptor")}")
            weatherListener.update(Weather(12, WeatherType.Rainy))
            weatherListener.update(Weather(24, WeatherType.Sunny))
        }
    }
}
