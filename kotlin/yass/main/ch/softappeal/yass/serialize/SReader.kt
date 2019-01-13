package ch.softappeal.yass.serialize

abstract class SReader {
    abstract suspend fun readByte(): Byte

    abstract suspend fun readBytes(buffer: ByteArray, offset: Int, length: Int)

    suspend fun readBytes(buffer: ByteArray) =
        readBytes(buffer, 0, buffer.size)

    suspend fun readShort(): Short = (
        ((readByte().toInt() and 0b1111_1111) shl 8) or
            ((readByte().toInt() and 0b1111_1111) shl 0)
        ).toShort()

    suspend fun readInt(): Int =
        ((readByte().toInt() and 0b1111_1111) shl 24) or
            ((readByte().toInt() and 0b1111_1111) shl 16) or
            ((readByte().toInt() and 0b1111_1111) shl 8) or
            ((readByte().toInt() and 0b1111_1111) shl 0)

    suspend fun readLong(): Long =
        ((readByte().toLong() and 0b1111_1111) shl 56) or
            ((readByte().toLong() and 0b1111_1111) shl 48) or
            ((readByte().toLong() and 0b1111_1111) shl 40) or
            ((readByte().toLong() and 0b1111_1111) shl 32) or
            ((readByte().toLong() and 0b1111_1111) shl 24) or
            ((readByte().toLong() and 0b1111_1111) shl 16) or
            ((readByte().toLong() and 0b1111_1111) shl 8) or
            ((readByte().toLong() and 0b1111_1111) shl 0)

    suspend fun readChar(): Char = (
        ((readByte().toInt() and 0b1111_1111) shl 8) or
            ((readByte().toInt() and 0b1111_1111) shl 0)
        ).toChar()

    suspend fun readFloat(): Float =
        java.lang.Float.intBitsToFloat(readInt())

    suspend fun readDouble(): Double =
        java.lang.Double.longBitsToDouble(readLong())

    suspend fun readVarInt(): Int {
        var shift = 0
        var value = 0
        while (shift < 32) {
            val b = readByte().toInt()
            value = value or ((b and 0b0111_1111) shl shift)
            if ((b and 0b1000_0000) == 0) return value
            shift += 7
        }
        error("malformed input")
    }

    suspend fun readZigZagInt(): Int {
        val value = readVarInt()
        return (value ushr 1) xor -(value and 0b0000_0001)
    }

    suspend fun readVarLong(): Long {
        var shift = 0
        var value = 0L
        while (shift < 64) {
            val b = readByte().toInt()
            value = value or ((b and 0b0111_1111).toLong() shl shift)
            if ((b and 0b1000_0000) == 0) return value
            shift += 7
        }
        error("malformed input")
    }

    suspend fun skipVar() {
        while ((readByte().toInt() and 0b1000_0000) != 0) Unit
    }

    suspend fun readZigZagLong(): Long {
        val value = readVarLong()
        return (value ushr 1) xor -(value and 0b0000_0001)
    }
}
