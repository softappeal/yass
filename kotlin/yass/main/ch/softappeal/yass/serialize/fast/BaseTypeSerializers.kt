@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.serialize.fast

import ch.softappeal.yass.serialize.Reader
import ch.softappeal.yass.serialize.Writer
import ch.softappeal.yass.serialize.utf8toBytes
import ch.softappeal.yass.serialize.utf8toString
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.util.Date

val BooleanSerializer = object : BaseTypeSerializer<Boolean>(Boolean::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readByte().toInt() != 0
    override fun write(writer: Writer, value: Boolean) = writer.writeByte(if (value) 1.toByte() else 0.toByte())
}

val ByteSerializer = object : BaseTypeSerializer<Byte>(Byte::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readByte()
    override fun write(writer: Writer, value: Byte) = writer.writeByte(value)
}

val ShortSerializer = object : BaseTypeSerializer<Short>(Short::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readZigZagInt().toShort()
    override fun write(writer: Writer, value: Short) = writer.writeZigZagInt(value.toInt())
}

val IntSerializer = object : BaseTypeSerializer<Int>(Int::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readZigZagInt()
    override fun write(writer: Writer, value: Int) = writer.writeZigZagInt(value)
}

val LongSerializer = object : BaseTypeSerializer<Long>(Long::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readZigZagLong()
    override fun write(writer: Writer, value: Long) = writer.writeZigZagLong(value)
}

val CharSerializer = object : BaseTypeSerializer<Char>(Char::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readChar()
    override fun write(writer: Writer, value: Char) = writer.writeChar(value)
}

val FloatSerializer = object : BaseTypeSerializer<Float>(Float::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readFloat()
    override fun write(writer: Writer, value: Float) = writer.writeFloat(value)
}

val DoubleSerializer = object : BaseTypeSerializer<Double>(Double::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readDouble()
    override fun write(writer: Writer, value: Double) = writer.writeDouble(value)
}

val BooleanArraySerializer = object : BaseTypeSerializer<BooleanArray>(BooleanArray::class.java) {
    override fun read(reader: Reader): BooleanArray {
        val length = reader.readVarInt()
        var value = BooleanArray(Math.min(length, 128))
        for (i in 0 until length) {
            if (i >= value.size) value = value.copyOf(Math.min(length, 2 * value.size))
            value[i] = reader.readByte().toInt() != 0
        }
        return value
    }

    override fun write(writer: Writer, value: BooleanArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeByte(if (v) 1.toByte() else 0.toByte())
    }
}

val ByteArraySerializer = object : BaseTypeSerializer<ByteArray>(ByteArray::class.java) {
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

val ShortArraySerializer = object : BaseTypeSerializer<ShortArray>(ShortArray::class.java) {
    override fun read(reader: Reader): ShortArray {
        val length = reader.readVarInt()
        var value = ShortArray(Math.min(length, 64))
        for (i in 0 until length) {
            if (i >= value.size) value = value.copyOf(Math.min(length, 2 * value.size))
            value[i] = reader.readZigZagInt().toShort()
        }
        return value
    }

    override fun write(writer: Writer, value: ShortArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeZigZagInt(v.toInt())
    }
}

val IntArraySerializer = object : BaseTypeSerializer<IntArray>(IntArray::class.java) {
    override fun read(reader: Reader): IntArray {
        val length = reader.readVarInt()
        var value = IntArray(Math.min(length, 32))
        for (i in 0 until length) {
            if (i >= value.size) value = value.copyOf(Math.min(length, 2 * value.size))
            value[i] = reader.readZigZagInt()
        }
        return value
    }

    override fun write(writer: Writer, value: IntArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeZigZagInt(v)
    }
}

val LongArraySerializer = object : BaseTypeSerializer<LongArray>(LongArray::class.java) {
    override fun read(reader: Reader): LongArray {
        val length = reader.readVarInt()
        var value = LongArray(Math.min(length, 16))
        for (i in 0 until length) {
            if (i >= value.size) value = value.copyOf(Math.min(length, 2 * value.size))
            value[i] = reader.readZigZagLong()
        }
        return value
    }

    override fun write(writer: Writer, value: LongArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeZigZagLong(v)
    }
}

val CharArraySerializer = object : BaseTypeSerializer<CharArray>(CharArray::class.java) {
    override fun read(reader: Reader): CharArray {
        val length = reader.readVarInt()
        var value = CharArray(Math.min(length, 64))
        for (i in 0 until length) {
            if (i >= value.size) value = value.copyOf(Math.min(length, 2 * value.size))
            value[i] = reader.readChar()
        }
        return value
    }

    override fun write(writer: Writer, value: CharArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeChar(v)
    }
}

val FloatArraySerializer = object : BaseTypeSerializer<FloatArray>(FloatArray::class.java) {
    override fun read(reader: Reader): FloatArray {
        val length = reader.readVarInt()
        var value = FloatArray(Math.min(length, 32))
        for (i in 0 until length) {
            if (i >= value.size) value = value.copyOf(Math.min(length, 2 * value.size))
            value[i] = reader.readFloat()
        }
        return value
    }

    override fun write(writer: Writer, value: FloatArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeFloat(v)
    }
}

val DoubleArraySerializer = object : BaseTypeSerializer<DoubleArray>(DoubleArray::class.java) {
    override fun read(reader: Reader): DoubleArray {
        val length = reader.readVarInt()
        var value = DoubleArray(Math.min(length, 16))
        for (i in 0 until length) {
            if (i >= value.size) value = value.copyOf(Math.min(length, 2 * value.size))
            value[i] = reader.readDouble()
        }
        return value
    }

    override fun write(writer: Writer, value: DoubleArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeDouble(v)
    }
}

val StringSerializer = object : BaseTypeSerializer<String>(String::class.java) {
    override fun read(reader: Reader) = utf8toString(ByteArraySerializer.read(reader))
    override fun write(writer: Writer, value: String) = ByteArraySerializer.write(writer, utf8toBytes(value))
}

val DateSerializer = object : BaseTypeSerializer<Date>(Date::class.java) {
    override fun read(reader: Reader) = Date(LongSerializer.read(reader))
    override fun write(writer: Writer, value: Date) = LongSerializer.write(writer, value.time)
}

val InstantSerializer = object : BaseTypeSerializer<Instant>(Instant::class.java) {
    override fun read(reader: Reader) = Instant.ofEpochSecond(LongSerializer.read(reader), reader.readVarInt().toLong())
    override fun write(writer: Writer, value: Instant) {
        LongSerializer.write(writer, value.epochSecond)
        writer.writeVarInt(value.nano)
    }
}

val BigIntegerSerializer = object : BaseTypeSerializer<BigInteger>(BigInteger::class.java) {
    override fun read(reader: Reader) = BigInteger(ByteArraySerializer.read(reader))
    override fun write(writer: Writer, value: BigInteger) = ByteArraySerializer.write(writer, value.toByteArray())
}

val BigDecimalSerializer = object : BaseTypeSerializer<BigDecimal>(BigDecimal::class.java) {
    override fun read(reader: Reader) = BigDecimal(BigIntegerSerializer.read(reader), IntSerializer.read(reader))
    override fun write(writer: Writer, value: BigDecimal) {
        BigIntegerSerializer.write(writer, value.unscaledValue())
        IntSerializer.write(writer, value.scale())
    }
}
