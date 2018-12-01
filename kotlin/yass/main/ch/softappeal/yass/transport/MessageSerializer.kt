package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.*

private const val Request = 0.toByte()
private const val ValueReply = 1.toByte()
private const val ExceptionReply = 2.toByte()

fun messageSerializer(contractSerializer: Serializer) = object : Serializer {
    override fun read(reader: Reader): Message = when (val type = reader.readByte()) {
        Request -> Request(
            reader.readZigZagInt(),
            reader.readZigZagInt(),
            contractSerializer.read(reader) as List<Any?>
        )
        ValueReply -> ValueReply(
            contractSerializer.read(reader)
        )
        ExceptionReply -> ExceptionReply(
            contractSerializer.read(reader) as Exception
        )
        else -> error("unexpected type $type")
    }

    override fun write(writer: Writer, value: Any?) = when (value) {
        is Request -> {
            writer.writeByte(Request)
            writer.writeZigZagInt(value.serviceId)
            writer.writeZigZagInt(value.methodId)
            contractSerializer.write(writer, value.arguments)
        }
        is ValueReply -> {
            writer.writeByte(ValueReply)
            contractSerializer.write(writer, value.value)
        }
        is ExceptionReply -> {
            writer.writeByte(ExceptionReply)
            contractSerializer.write(writer, value.exception)
        }
        else -> error("unexpected value '$value'")
    }
}
