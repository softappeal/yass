package ch.softappeal.yass.tutorial.contract

import ch.softappeal.yass.remote.OneWay
import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Writer
import ch.softappeal.yass.serialize.fast.BaseTypeSerializer
import ch.softappeal.yass.serialize.fast.IntSerializer

abstract class Instrument protected constructor(val id: Int, val name: String)

/** Shows how to use graphs. */
class Node(val id: Double) {
    val links = mutableListOf<Node>()
    var next: Node? = null
}

enum class PriceKind { BID, ASK }

class Price(val instrumentId: Int, val value: Int, val kind: PriceKind)

class Expiration(val year: Int, val month: Int, val day: Int)

/** Shows how to use a contract internal base type. */
val ExpirationSerializer = object : BaseTypeSerializer<Expiration>(Expiration::class.java) {
    override fun read(reader: Reader): Expiration {
        return Expiration(
            IntSerializer.read(reader),
            IntSerializer.read(reader),
            IntSerializer.read(reader)
        )
    }

    override fun write(writer: Writer, value: Expiration) {
        IntSerializer.write(writer, value.year)
        IntSerializer.write(writer, value.month)
        IntSerializer.write(writer, value.day)
    }
}

class SystemException(val details: String) : RuntimeException() {
    override val message: String get() = details
}

abstract class ApplicationException : Exception()

class UnknownInstrumentsException(val instrumentIds: List<Int>) : ApplicationException() {
    var onlyNeededForTests1: Any? = null
    var onlyNeededForTests2: ByteArray? = null
    var onlyNeededForTests3: Throwable? = null
    override val message: String get() = "there are " + instrumentIds.size + " unknown instruments"
}

interface EchoService {
    fun echo(value: Any?): Any?
}

interface PriceEngine {
    fun subscribe(instrumentIds: List<Int>)
}

interface PriceListener {
    @OneWay
    fun newPrices(prices: List<Price>)
}
