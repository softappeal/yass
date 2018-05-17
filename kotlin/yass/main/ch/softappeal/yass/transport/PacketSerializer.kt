@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.Message
import ch.softappeal.yass.remote.session.END_PACKET
import ch.softappeal.yass.remote.session.END_REQUEST_NUMBER
import ch.softappeal.yass.remote.session.Packet
import ch.softappeal.yass.remote.session.isEndPacket
import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Serializer
import ch.softappeal.yass.serialize.Writer

fun PacketSerializer(messageSerializer: Serializer) = object : Serializer {
    override fun read(reader: Reader): Packet {
        val requestNumber = reader.readInt()
        return if (isEndPacket(requestNumber)) END_PACKET else Packet(requestNumber, messageSerializer.read(reader) as Message)
    }

    override fun write(writer: Writer, value: Any?) {
        val packet = value as Packet
        if (packet.isEnd())
            writer.writeInt(END_REQUEST_NUMBER)
        else {
            writer.writeInt(packet.requestNumber())
            messageSerializer.write(writer, packet.message())
        }
    }
}
