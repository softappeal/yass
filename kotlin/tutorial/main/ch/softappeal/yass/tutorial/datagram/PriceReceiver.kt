package ch.softappeal.yass.tutorial.datagram

import ch.softappeal.yass.remote.Server
import ch.softappeal.yass.remote.Service
import ch.softappeal.yass.transport.ServerTransport
import ch.softappeal.yass.tutorial.contract.Initiator
import ch.softappeal.yass.tutorial.contract.MessageSerializer
import ch.softappeal.yass.tutorial.contract.Price
import ch.softappeal.yass.tutorial.contract.PriceListener
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.StandardProtocolFamily
import java.net.StandardSocketOptions
import java.nio.channels.DatagramChannel

fun main(args: Array<String>) {
    val transport = ServerTransport(
        Server(Service(Initiator.priceListener, object : PriceListener {
            override fun newPrices(prices: List<Price>) {
                val price = prices[0]
                println("received ${price.kind}: ${price.value}")
            }
        })),
        MessageSerializer
    )
    val channel = DatagramChannel.open(StandardProtocolFamily.INET)
        .setOption(StandardSocketOptions.SO_REUSEADDR, true)
        .bind(InetSocketAddress(Port))
    val networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost())
    channel.join(InetAddress.getByName(GroupBid), networkInterface)
    channel.join(InetAddress.getByName(GroupAsk), networkInterface)
    while (true) invoke(transport, channel, 128)
}
