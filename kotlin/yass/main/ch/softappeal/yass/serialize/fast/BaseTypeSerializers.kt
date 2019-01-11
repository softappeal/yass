package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.serialize.fast.FieldType.*

val BooleanSerializer = object : BaseTypeSerializer<Boolean>(Boolean::class, VarInt) {
    override fun read(reader: Reader) = reader.readByte().toInt() != 0
    override fun write(writer: Writer, value: Boolean) = writer.writeByte((if (value) 1 else 0).toByte())
}

val ByteSerializer = object : BaseTypeSerializer<Byte>(Byte::class, VarInt) {
    override fun read(reader: Reader) = reader.readZigZagInt().toByte()
    override fun write(writer: Writer, value: Byte) = writer.writeZigZagInt(value.toInt())
}

val ShortSerializer = object : BaseTypeSerializer<Short>(Short::class, VarInt) {
    override fun read(reader: Reader) = reader.readZigZagInt().toShort()
    override fun write(writer: Writer, value: Short) = writer.writeZigZagInt(value.toInt())
}

val IntSerializer = object : BaseTypeSerializer<Int>(Int::class, VarInt) {
    override fun read(reader: Reader) = reader.readZigZagInt()
    override fun write(writer: Writer, value: Int) = writer.writeZigZagInt(value)
}

val LongSerializer = object : BaseTypeSerializer<Long>(Long::class, VarInt) {
    override fun read(reader: Reader) = reader.readZigZagLong()
    override fun write(writer: Writer, value: Long) = writer.writeZigZagLong(value)
}

val CharSerializer = object : BaseTypeSerializer<Char>(Char::class, VarInt) {
    override fun read(reader: Reader): Char = reader.readVarInt().toChar()
    override fun write(writer: Writer, value: Char) = writer.writeVarInt(value.toInt())
}

val FloatSerializer = object : BaseTypeSerializer<Float>(Float::class, Binary) {
    override fun read(reader: Reader): Float {
        reader.readByte()
        return reader.readFloat()
    }

    override fun write(writer: Writer, value: Float) {
        writer.writeByte(4)
        writer.writeFloat(value)
    }
}

val FloatSerializerNoSkipping = object : BaseTypeSerializer<Float>(Float::class, Binary) {
    override fun read(reader: Reader) = reader.readFloat()
    override fun write(writer: Writer, value: Float) = writer.writeFloat(value)
}

val DoubleSerializer = object : BaseTypeSerializer<Double>(Double::class, Binary) {
    override fun read(reader: Reader): Double {
        reader.readByte()
        return reader.readDouble()
    }

    override fun write(writer: Writer, value: Double) {
        writer.writeByte(8)
        writer.writeDouble(value)
    }
}

val DoubleSerializerNoSkipping = object : BaseTypeSerializer<Double>(Double::class, Binary) {
    override fun read(reader: Reader) = reader.readDouble()
    override fun write(writer: Writer, value: Double) = writer.writeDouble(value)
}

val BinarySerializer = object : BaseTypeSerializer<ByteArray>(ByteArray::class, Binary) {
    override fun read(reader: Reader): ByteArray {
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

    override fun write(writer: Writer, value: ByteArray) {
        writer.writeVarInt(value.size)
        writer.writeBytes(value)
    }
}

val StringSerializer = object : BaseTypeSerializer<String>(String::class, BinarySerializer.fieldType) {
    override fun read(reader: Reader) = utf8toString(BinarySerializer.read(reader))
    override fun write(writer: Writer, value: String) = BinarySerializer.write(writer, utf8toBytes(value))
}
