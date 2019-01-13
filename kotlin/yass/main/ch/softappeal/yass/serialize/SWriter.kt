package ch.softappeal.yass.serialize

abstract class SWriter {
    abstract suspend fun writeByte(value: Byte)

    abstract suspend fun writeBytes(buffer: ByteArray, offset: Int, length: Int)

    suspend fun writeBytes(buffer: ByteArray) =
        writeBytes(buffer, 0, buffer.size)

    suspend fun writeShort(value: Short) {
        writeByte((value.toInt() shr 8).toByte())
        writeByte((value.toInt() shr 0).toByte())
    }

    suspend fun writeInt(value: Int) {
        writeByte((value shr 24).toByte())
        writeByte((value shr 16).toByte())
        writeByte((value shr 8).toByte())
        writeByte((value shr 0).toByte())
    }

    suspend fun writeLong(value: Long) {
        writeByte((value shr 56).toByte())
        writeByte((value shr 48).toByte())
        writeByte((value shr 40).toByte())
        writeByte((value shr 32).toByte())
        writeByte((value shr 24).toByte())
        writeByte((value shr 16).toByte())
        writeByte((value shr 8).toByte())
        writeByte((value shr 0).toByte())
    }

    suspend fun writeChar(value: Char) {
        writeByte((value.toInt() shr 8).toByte())
        writeByte((value.toInt() shr 0).toByte())
    }

    suspend fun writeFloat(value: Float) =
        writeInt(java.lang.Float.floatToRawIntBits(value))

    suspend fun writeDouble(value: Double) =
        writeLong(java.lang.Double.doubleToRawLongBits(value))

    suspend fun writeVarInt(v: Int) {
        var value = v
        while (true) {
            if ((value and 0b0111_1111.inv()) == 0) {
                writeByte(value.toByte())
                return
            }
            writeByte(((value and 0b0111_1111) or 0b1000_0000).toByte())
            value = value ushr 7
        }
    }

    suspend fun writeZigZagInt(value: Int) =
        writeVarInt((value shl 1) xor (value shr 31))

    suspend fun writeVarLong(v: Long) {
        var value = v
        while (true) {
            if ((value and 0b0111_1111.inv()) == 0L) {
                writeByte(value.toByte())
                return
            }
            writeByte(((value and 0b0111_1111) or 0b1000_0000).toByte())
            value = value ushr 7
        }
    }

    suspend fun writeZigZagLong(value: Long) =
        writeVarLong((value shl 1) xor (value shr 63))
}
