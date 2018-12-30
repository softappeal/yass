package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.serialize.*

val BooleanSerializer = object : BaseTypeSerializer<Boolean>(Boolean::class.javaObjectType, WireType.Bytes1) {
    override fun read(reader: Reader) = reader.readByte().toInt() != 0
    override fun write(writer: Writer, value: Boolean) = writer.writeByte(if (value) 1.toByte() else 0.toByte())
}

val ByteSerializer = object : BaseTypeSerializer<Byte>(Byte::class.javaObjectType, WireType.Bytes1) {
    override fun read(reader: Reader) = reader.readByte()
    override fun write(writer: Writer, value: Byte) = writer.writeByte(value)
}

val ShortSerializer = object : BaseTypeSerializer<Short>(Short::class.javaObjectType, WireType.VarInt) {
    override fun read(reader: Reader) = reader.readZigZagInt().toShort()
    override fun write(writer: Writer, value: Short) = writer.writeZigZagInt(value.toInt())
}

val IntSerializer = object : BaseTypeSerializer<Int>(Int::class.javaObjectType, WireType.VarInt) {
    override fun read(reader: Reader) = reader.readZigZagInt()
    override fun write(writer: Writer, value: Int) = writer.writeZigZagInt(value)
}

val LongSerializer = object : BaseTypeSerializer<Long>(Long::class.javaObjectType, WireType.VarInt) {
    override fun read(reader: Reader) = reader.readZigZagLong()
    override fun write(writer: Writer, value: Long) = writer.writeZigZagLong(value)
}

val CharSerializer = object : BaseTypeSerializer<Char>(Char::class.javaObjectType, WireType.Bytes2) {
    override fun read(reader: Reader) = reader.readChar()
    override fun write(writer: Writer, value: Char) = writer.writeChar(value)
}

val FloatSerializer = object : BaseTypeSerializer<Float>(Float::class.javaObjectType, WireType.Bytes4) {
    override fun read(reader: Reader) = reader.readFloat()
    override fun write(writer: Writer, value: Float) = writer.writeFloat(value)
}

val DoubleSerializer = object : BaseTypeSerializer<Double>(Double::class.javaObjectType, WireType.Bytes8) {
    override fun read(reader: Reader) = reader.readDouble()
    override fun write(writer: Writer, value: Double) = writer.writeDouble(value)
}

val ByteArraySerializer = object : BaseTypeSerializer<ByteArray>(ByteArray::class.java, WireType.Binary) {
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

val StringSerializer = object : BaseTypeSerializer<String>(String::class.java, ByteArraySerializer.wireType) {
    override fun read(reader: Reader) = utf8toString(ByteArraySerializer.read(reader))
    override fun write(writer: Writer, value: String) = ByteArraySerializer.write(writer, utf8toBytes(value))
}
