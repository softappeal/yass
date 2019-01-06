package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.serialize.*

val BooleanSerializer = object : BaseTypeSerializer<Boolean>(Boolean::class.javaObjectType, FieldType.VarInt) {
    override fun read(reader: Reader) = reader.readByte().toInt() != 0
    override fun write(writer: Writer, value: Boolean) = writer.writeByte((if (value) 1 else 0).toByte())
}

val ShortSerializer = object : BaseTypeSerializer<Short>(Short::class.javaObjectType, FieldType.VarInt) {
    override fun read(reader: Reader) = reader.readZigZagInt().toShort()
    override fun write(writer: Writer, value: Short) = writer.writeZigZagInt(value.toInt())
}

val IntSerializer = object : BaseTypeSerializer<Int>(Int::class.javaObjectType, FieldType.VarInt) {
    override fun read(reader: Reader) = reader.readZigZagInt()
    override fun write(writer: Writer, value: Int) = writer.writeZigZagInt(value)
}

val LongSerializer = object : BaseTypeSerializer<Long>(Long::class.javaObjectType, FieldType.VarInt) {
    override fun read(reader: Reader) = reader.readZigZagLong()
    override fun write(writer: Writer, value: Long) = writer.writeZigZagLong(value)
}

val ByteArraySerializer = object : BaseTypeSerializer<ByteArray>(ByteArray::class.java, FieldType.Binary) {
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

val StringSerializer = object : BaseTypeSerializer<String>(String::class.java, ByteArraySerializer.fieldType) {
    override fun read(reader: Reader) = utf8toString(ByteArraySerializer.read(reader))
    override fun write(writer: Writer, value: String) = ByteArraySerializer.write(writer, utf8toBytes(value))
}
