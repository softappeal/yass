@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.Message
import ch.softappeal.yass.remote.session.EndPacket
import ch.softappeal.yass.remote.session.EndRequestNumber
import ch.softappeal.yass.remote.session.Packet
import ch.softappeal.yass.remote.session.isEndPacket
import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Serializer
import ch.softappeal.yass.serialize.Writer

fun packetSerializer(messageSerializer: Serializer) = object : Serializer {
    override fun read(reader: Reader): Packet {
        val requestNumber = reader.readInt()
        return if (isEndPacket(requestNumber)) EndPacket else Packet(requestNumber, messageSerializer.read(reader) as Message)
    }

    override fun write(writer: Writer, value: Any?) {
        val packet = value as Packet
        if (packet.isEnd())
            writer.writeInt(EndRequestNumber)
        else {
            writer.writeInt(packet.requestNumber())
            messageSerializer.write(writer, packet.message())
        }
    }
}
