package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.serialize.fast.SFieldType.*

val SBooleanSerializer = object : SBaseTypeSerializer<Boolean>(Boolean::class, VarInt) {
    override suspend fun read(reader: SReader) = reader.readByte().toInt() != 0
    override suspend fun write(writer: SWriter, value: Boolean) = writer.writeByte((if (value) 1 else 0).toByte())
}

val SByteSerializer = object : SBaseTypeSerializer<Byte>(Byte::class, VarInt) {
    override suspend fun read(reader: SReader) = reader.readZigZagInt().toByte()
    override suspend fun write(writer: SWriter, value: Byte) = writer.writeZigZagInt(value.toInt())
}

val SShortSerializer = object : SBaseTypeSerializer<Short>(Short::class, VarInt) {
    override suspend fun read(reader: SReader) = reader.readZigZagInt().toShort()
    override suspend fun write(writer: SWriter, value: Short) = writer.writeZigZagInt(value.toInt())
}

val SIntSerializer = object : SBaseTypeSerializer<Int>(Int::class, VarInt) {
    override suspend fun read(reader: SReader) = reader.readZigZagInt()
    override suspend fun write(writer: SWriter, value: Int) = writer.writeZigZagInt(value)
}

val SLongSerializer = object : SBaseTypeSerializer<Long>(Long::class, VarInt) {
    override suspend fun read(reader: SReader) = reader.readZigZagLong()
    override suspend fun write(writer: SWriter, value: Long) = writer.writeZigZagLong(value)
}

val SCharSerializer = object : SBaseTypeSerializer<Char>(Char::class, VarInt) {
    override suspend fun read(reader: SReader): Char = reader.readVarInt().toChar()
    override suspend fun write(writer: SWriter, value: Char) = writer.writeVarInt(value.toInt())
}

val SFloatSerializer = object : SBaseTypeSerializer<Float>(Float::class, Binary) {
    override suspend fun read(reader: SReader): Float {
        reader.readByte()
        return reader.readFloat()
    }

    override suspend fun write(writer: SWriter, value: Float) {
        writer.writeByte(4)
        writer.writeFloat(value)
    }
}

val SFloatSerializerNoSkipping = object : SBaseTypeSerializer<Float>(Float::class, Binary) {
    override suspend fun read(reader: SReader) = reader.readFloat()
    override suspend fun write(writer: SWriter, value: Float) = writer.writeFloat(value)
}

val SDoubleSerializer = object : SBaseTypeSerializer<Double>(Double::class, Binary) {
    override suspend fun read(reader: SReader): Double {
        reader.readByte()
        return reader.readDouble()
    }

    override suspend fun write(writer: SWriter, value: Double) {
        writer.writeByte(8)
        writer.writeDouble(value)
    }
}

val SDoubleSerializerNoSkipping = object : SBaseTypeSerializer<Double>(Double::class, Binary) {
    override suspend fun read(reader: SReader) = reader.readDouble()
    override suspend fun write(writer: SWriter, value: Double) = writer.writeDouble(value)
}

val SBinarySerializer = object : SBaseTypeSerializer<ByteArray>(ByteArray::class, Binary) {
    override suspend fun read(reader: SReader): ByteArray {
        val length = reader.readVarInt()
        var value = ByteArray(Math.min(length, 128))
        var i = 0
        while (i < length) {
            if (i >= value.size) value = value.copyOf(Math.min(length, 2 * value.size))
            val l = value.size - i
            reader.readBytes(value, i, l)
            i += l
        }
        return value
    }

    override suspend fun write(writer: SWriter, value: ByteArray) {
        writer.writeVarInt(value.size)
        writer.writeBytes(value)
    }
}

val SStringSerializer = object : SBaseTypeSerializer<String>(String::class, SBinarySerializer.fieldType) {
    override suspend fun read(reader: SReader) = utf8toString(SBinarySerializer.read(reader))
    override suspend fun write(writer: SWriter, value: String) = SBinarySerializer.write(writer, utf8toBytes(value))
}
