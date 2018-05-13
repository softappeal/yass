package ch.softappeal.yass.serialize

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

abstract class Writer {
    abstract fun writeByte(value: Byte)
    abstract fun writeBytes(buffer: ByteArray, offset: Int, length: Int)
    fun writeBytes(buffer: ByteArray) = writeBytes(buffer, 0, buffer.size)
    fun writeShort(value: Short) {
        writeByte((value.toInt() shr 8).toByte())
        writeByte((value.toInt() shr 0).toByte())
    }

    fun writeInt(value: Int) {
        writeByte((value shr 24).toByte())
        writeByte((value shr 16).toByte())
        writeByte((value shr 8).toByte())
        writeByte((value shr 0).toByte())
    }

    fun writeLong(value: Long) {
        writeByte((value shr 56).toByte())
        writeByte((value shr 48).toByte())
        writeByte((value shr 40).toByte())
        writeByte((value shr 32).toByte())
        writeByte((value shr 24).toByte())
        writeByte((value shr 16).toByte())
        writeByte((value shr 8).toByte())
        writeByte((value shr 0).toByte())
    }

    fun writeChar(value: Char) {
        writeByte((value.toInt() shr 8).toByte())
        writeByte((value.toInt() shr 0).toByte())
    }

    fun writeFloat(value: Float) = writeInt(java.lang.Float.floatToRawIntBits(value))
    fun writeDouble(value: Double) = writeLong(java.lang.Double.doubleToRawLongBits(value))
    fun writeVarInt(v: Int) {
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

    fun writeZigZagInt(value: Int) = writeVarInt((value shl 1) xor (value shr 31))
    fun writeVarLong(v: Long) {
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

    fun writeZigZagLong(value: Long) = writeVarLong((value shl 1) xor (value shr 63))
    fun stream() = object : OutputStream() {
        override fun write(i: Int) = writeByte(i.toByte())
        override fun write(b: ByteArray, off: Int, len: Int) = writeBytes(b, off, len)
    }
}

fun writer(out: OutputStream) = object : Writer() {
    override fun writeByte(value: Byte) = out.write(value.toInt())
    override fun writeBytes(buffer: ByteArray, offset: Int, length: Int) = out.write(buffer, offset, length)
}

class ByteBufferOutputStream(size: Int) : ByteArrayOutputStream(size) {
    fun toByteBuffer() = ByteBuffer.wrap(buf, 0, count)
}

fun utf8toBytes(value: String): ByteArray = value.toByteArray(StandardCharsets.UTF_8)
