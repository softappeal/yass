package ch.softappeal.yass.tutorial.datagram

import ch.softappeal.yass.remote.Client
import ch.softappeal.yass.remote.ClientInvocation
import ch.softappeal.yass.remote.MethodMapping
import ch.softappeal.yass.remote.Request
import ch.softappeal.yass.serialize.ByteBufferOutputStream
import ch.softappeal.yass.serialize.Serializer
import ch.softappeal.yass.serialize.reader
import ch.softappeal.yass.serialize.writer
import ch.softappeal.yass.transport.ServerTransport
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

private fun checkOneWay(methodMapping: MethodMapping, request: Request) {
    require(methodMapping.oneWay) {
        "transport not allowed for rpc method (serviceId ${request.serviceId}, methodId ${request.methodId})"
    }
}

fun client(messageSerializer: Serializer, channel: DatagramChannel, target: SocketAddress) = object : Client() {
    override fun invoke(invocation: ClientInvocation) {
        invocation.invoke(false) { request ->
            checkOneWay(invocation.methodMapping, request)
            val out = ByteBufferOutputStream(128)
            messageSerializer.write(writer(out), request)
            channel.send(out.toByteBuffer(), target)
        }
    }
}

fun invoke(setup: ServerTransport, channel: DatagramChannel, maxRequestBytes: Int) {
    val input = ByteBuffer.allocate(maxRequestBytes)
    channel.receive(input)
    input.flip()
    val request = setup.read(reader(input))
    check(!input.hasRemaining()) { "input buffer is not empty" }
    val invocation = setup.invocation(false, request)
    checkOneWay(invocation.methodMapping, request)
    invocation.invoke { _ -> }
}
