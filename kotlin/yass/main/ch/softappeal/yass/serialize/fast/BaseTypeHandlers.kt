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
import java.util.Arrays
import java.util.Date

val BTH_BOOLEAN = object : BaseTypeHandler<Boolean>(Boolean::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readByte().toInt() != 0
    override fun write(writer: Writer, value: Boolean) = writer.writeByte(if (value) 1.toByte() else 0.toByte())
}

val BTH_BYTE = object : BaseTypeHandler<Byte>(Byte::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readByte()
    override fun write(writer: Writer, value: Byte) = writer.writeByte(value)
}

val BTH_SHORT = object : BaseTypeHandler<Short>(Short::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readZigZagInt().toShort()
    override fun write(writer: Writer, value: Short) = writer.writeZigZagInt(value.toInt())
}

val BTH_INTEGER = object : BaseTypeHandler<Int>(Int::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readZigZagInt()
    override fun write(writer: Writer, value: Int) = writer.writeZigZagInt(value)
}

val BTH_LONG = object : BaseTypeHandler<Long>(Long::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readZigZagLong()
    override fun write(writer: Writer, value: Long) = writer.writeZigZagLong(value)
}

val BTH_CHARACTER = object : BaseTypeHandler<Char>(Char::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readChar()
    override fun write(writer: Writer, value: Char) = writer.writeChar(value)
}

val BTH_FLOAT = object : BaseTypeHandler<Float>(Float::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readFloat()
    override fun write(writer: Writer, value: Float) = writer.writeFloat(value)
}

val BTH_DOUBLE = object : BaseTypeHandler<Double>(Double::class.javaObjectType) {
    override fun read(reader: Reader) = reader.readDouble()
    override fun write(writer: Writer, value: Double) = writer.writeDouble(value)
}

val BTH_BOOLEAN_ARRAY = object : BaseTypeHandler<BooleanArray>(BooleanArray::class.java) {
    override fun read(reader: Reader): BooleanArray {
        val length = reader.readVarInt()
        var value = BooleanArray(Math.min(length, 128))
        for (i in 0 until length) {
            if (i >= value.size) value = Arrays.copyOf(value, Math.min(length, 2 * value.size))
            value[i] = reader.readByte().toInt() != 0
        }
        return value
    }

    override fun write(writer: Writer, value: BooleanArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeByte(if (v) 1.toByte() else 0.toByte())
    }
}

val BTH_BYTE_ARRAY = object : BaseTypeHandler<ByteArray>(ByteArray::class.java) {
    override fun read(reader: Reader): ByteArray {
        val length = reader.readVarInt()
        var value = ByteArray(Math.min(length, 128))
        var i = 0
        while (i < length) {
            if (i >= value.size) value = Arrays.copyOf(value, Math.min(length, 2 * value.size))
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

val BTH_SHORT_ARRAY = object : BaseTypeHandler<ShortArray>(ShortArray::class.java) {
    override fun read(reader: Reader): ShortArray {
        val length = reader.readVarInt()
        var value = ShortArray(Math.min(length, 64))
        for (i in 0 until length) {
            if (i >= value.size) value = Arrays.copyOf(value, Math.min(length, 2 * value.size))
            value[i] = reader.readZigZagInt().toShort()
        }
        return value
    }

    override fun write(writer: Writer, value: ShortArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeZigZagInt(v.toInt())
    }
}

val BTH_INTEGER_ARRAY = object : BaseTypeHandler<IntArray>(IntArray::class.java) {
    override fun read(reader: Reader): IntArray {
        val length = reader.readVarInt()
        var value = IntArray(Math.min(length, 32))
        for (i in 0 until length) {
            if (i >= value.size) value = Arrays.copyOf(value, Math.min(length, 2 * value.size))
            value[i] = reader.readZigZagInt()
        }
        return value
    }

    override fun write(writer: Writer, value: IntArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeZigZagInt(v)
    }
}

val BTH_LONG_ARRAY = object : BaseTypeHandler<LongArray>(LongArray::class.java) {
    override fun read(reader: Reader): LongArray {
        val length = reader.readVarInt()
        var value = LongArray(Math.min(length, 16))
        for (i in 0 until length) {
            if (i >= value.size) value = Arrays.copyOf(value, Math.min(length, 2 * value.size))
            value[i] = reader.readZigZagLong()
        }
        return value
    }

    override fun write(writer: Writer, value: LongArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeZigZagLong(v)
    }
}

val BTH_CHARACTER_ARRAY = object : BaseTypeHandler<CharArray>(CharArray::class.java) {
    override fun read(reader: Reader): CharArray {
        val length = reader.readVarInt()
        var value = CharArray(Math.min(length, 64))
        for (i in 0 until length) {
            if (i >= value.size) value = Arrays.copyOf(value, Math.min(length, 2 * value.size))
            value[i] = reader.readChar()
        }
        return value
    }

    override fun write(writer: Writer, value: CharArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeChar(v)
    }
}

val BTH_FLOAT_ARRAY = object : BaseTypeHandler<FloatArray>(FloatArray::class.java) {
    override fun read(reader: Reader): FloatArray {
        val length = reader.readVarInt()
        var value = FloatArray(Math.min(length, 32))
        for (i in 0 until length) {
            if (i >= value.size) value = Arrays.copyOf(value, Math.min(length, 2 * value.size))
            value[i] = reader.readFloat()
        }
        return value
    }

    override fun write(writer: Writer, value: FloatArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeFloat(v)
    }
}

val BTH_DOUBLE_ARRAY = object : BaseTypeHandler<DoubleArray>(DoubleArray::class.java) {
    override fun read(reader: Reader): DoubleArray {
        val length = reader.readVarInt()
        var value = DoubleArray(Math.min(length, 16))
        for (i in 0 until length) {
            if (i >= value.size) value = Arrays.copyOf(value, Math.min(length, 2 * value.size))
            value[i] = reader.readDouble()
        }
        return value
    }

    override fun write(writer: Writer, value: DoubleArray) {
        writer.writeVarInt(value.size)
        for (v in value) writer.writeDouble(v)
    }
}

val BTH_STRING = object : BaseTypeHandler<String>(String::class.java) {
    override fun read(reader: Reader) = utf8toString(BTH_BYTE_ARRAY.read(reader))
    override fun write(writer: Writer, value: String) = BTH_BYTE_ARRAY.write(writer, utf8toBytes(value))
}

val BTH_DATE = object : BaseTypeHandler<Date>(Date::class.java) {
    override fun read(reader: Reader) = Date(BTH_LONG.read(reader))
    override fun write(writer: Writer, value: Date) = BTH_LONG.write(writer, value.time)
}

val BTH_INSTANT = object : BaseTypeHandler<Instant>(Instant::class.java) {
    override fun read(reader: Reader) = Instant.ofEpochSecond(BTH_LONG.read(reader), reader.readVarInt().toLong())
    override fun write(writer: Writer, value: Instant) {
        BTH_LONG.write(writer, value.epochSecond)
        writer.writeVarInt(value.nano)
    }
}

val BTH_BIGINTEGER = object : BaseTypeHandler<BigInteger>(BigInteger::class.java) {
    override fun read(reader: Reader) = BigInteger(BTH_BYTE_ARRAY.read(reader))
    override fun write(writer: Writer, value: BigInteger) = BTH_BYTE_ARRAY.write(writer, value.toByteArray())
}

val BTH_BIGDECIMAL = object : BaseTypeHandler<BigDecimal>(BigDecimal::class.java) {
    override fun read(reader: Reader) = BigDecimal(BTH_BIGINTEGER.read(reader), BTH_INTEGER.read(reader))
    override fun write(writer: Writer, value: BigDecimal) {
        BTH_BIGINTEGER.write(writer, value.unscaledValue())
        BTH_INTEGER.write(writer, value.scale())
    }
}
