package ch.softappeal.yass.transport

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.*

fun sMessageSerializer(contractSerializer: SSerializer) = object : SSerializer {
    override suspend fun read(reader: SReader): Message = when (val type = reader.readByte()) {
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

    override suspend fun write(writer: SWriter, value: Any?) = when (value) {
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
