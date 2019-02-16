package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.remote.session.*
import ch.softappeal.yass.serialize.*

fun sPacketSerializer(messageSerializer: SSerializer) = object : SSerializer {
    override suspend fun read(reader: SReader): Packet {
        val requestNumber = reader.readInt()
        return if (isEndPacket(requestNumber)) EndPacket else
            Packet(requestNumber, messageSerializer.read(reader) as Message)
    }

    override suspend fun write(writer: SWriter, value: Any?) {
        val packet = value as Packet
        if (packet.isEnd)
            writer.writeInt(EndRequestNumber)
        else {
            writer.writeInt(packet.requestNumber)
            messageSerializer.write(writer, packet.message)
        }
    }
}
