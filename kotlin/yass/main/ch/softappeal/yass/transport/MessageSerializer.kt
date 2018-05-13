package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.ExceptionReply
import ch.softappeal.yass.remote.Message
import ch.softappeal.yass.remote.Request
import ch.softappeal.yass.remote.ValueReply
import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Serializer
import ch.softappeal.yass.serialize.Writer

private const val REQUEST = 0.toByte()
private const val VALUE_REPLY = 1.toByte()
private const val EXCEPTION_REPLY = 2.toByte()

fun MessageSerializer(contractSerializer: Serializer) = object : Serializer {
    override fun read(reader: Reader): Message {
        val type = reader.readByte()
        return when (type) {
            REQUEST -> Request(
                reader.readZigZagInt(),
                reader.readZigZagInt(),
                contractSerializer.read(reader) as List<Any?>
            )
            VALUE_REPLY -> ValueReply(
                contractSerializer.read(reader)
            )
            EXCEPTION_REPLY -> ExceptionReply(
                contractSerializer.read(reader) as Exception
            )
            else -> error("unexpected type $type")
        }
    }

    override fun write(writer: Writer, value: Any?) = when (value) {
        is Request -> {
            writer.writeByte(REQUEST)
            writer.writeZigZagInt(value.serviceId)
            writer.writeZigZagInt(value.methodId)
            contractSerializer.write(writer, value.arguments)
        }
        is ValueReply -> {
            writer.writeByte(VALUE_REPLY)
            contractSerializer.write(writer, value.value)
        }
        is ExceptionReply -> {
            writer.writeByte(EXCEPTION_REPLY)
            contractSerializer.write(writer, value.exception)
        }
        else -> error("unexpected value '$value'")
    }
}
