package ch.softappeal.yass.tutorial.datagram

import ch.softappeal.yass.tutorial.contract.Initiator
import ch.softappeal.yass.tutorial.contract.MessageSerializer
import ch.softappeal.yass.tutorial.contract.Price
import ch.softappeal.yass.tutorial.contract.PriceKind
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeUnit

const val GroupBid = "239.1.1.1"
const val GroupAsk = "239.1.1.2"
const val Port = 8989

fun main(args: Array<String>) {
    val channel = DatagramChannel.open()
    val clientBid = client(MessageSerializer, channel, InetSocketAddress(GroupBid, Port))
    val clientAsk = client(MessageSerializer, channel, InetSocketAddress(GroupAsk, Port))
    val priceListenerBid = clientBid.proxy(Initiator.priceListener)
    val priceListenerAsk = clientAsk.proxy(Initiator.priceListener)
    var value = 1
    while (true) {
        println("sending BID and ASK: $value")
        priceListenerBid.newPrices(listOf(Price(123, value, PriceKind.BID)))
        priceListenerAsk.newPrices(listOf(Price(123, value, PriceKind.ASK)))
        TimeUnit.MILLISECONDS.sleep(100)
        value++
    }
}
