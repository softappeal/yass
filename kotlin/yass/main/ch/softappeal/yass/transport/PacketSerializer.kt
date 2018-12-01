package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.remote.session.*
import ch.softappeal.yass.serialize.*

fun packetSerializer(messageSerializer: Serializer) = object : Serializer {
    override fun read(reader: Reader): Packet {
        val requestNumber = reader.readInt()
        return if (isEndPacket(requestNumber))
            EndPacket
        else
            Packet(requestNumber, messageSerializer.read(reader) as Message)
    }

    override fun write(writer: Writer, value: Any?) {
        val packet = value as Packet
        if (packet.isEnd)
            writer.writeInt(EndRequestNumber)
        else {
            writer.writeInt(packet.requestNumber)
            messageSerializer.write(writer, packet.message)
        }
    }
}
