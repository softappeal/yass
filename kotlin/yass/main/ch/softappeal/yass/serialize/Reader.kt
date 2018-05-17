@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.serialize

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

abstract class Reader {
    abstract fun readByte(): Byte
    abstract fun readBytes(buffer: ByteArray, offset: Int, length: Int)
    fun readBytes(buffer: ByteArray) = readBytes(buffer, 0, buffer.size)
    fun readShort(): Short = (
        ((readByte().toInt() and 0b1111_1111) shl 8) or
            ((readByte().toInt() and 0b1111_1111) shl 0)
        ).toShort()

    fun readInt(): Int =
        ((readByte().toInt() and 0b1111_1111) shl 24) or
            ((readByte().toInt() and 0b1111_1111) shl 16) or
            ((readByte().toInt() and 0b1111_1111) shl 8) or
            ((readByte().toInt() and 0b1111_1111) shl 0)

    fun readLong(): Long =
        ((readByte().toLong() and 0b1111_1111) shl 56) or
            ((readByte().toLong() and 0b1111_1111) shl 48) or
            ((readByte().toLong() and 0b1111_1111) shl 40) or
            ((readByte().toLong() and 0b1111_1111) shl 32) or
            ((readByte().toLong() and 0b1111_1111) shl 24) or
            ((readByte().toLong() and 0b1111_1111) shl 16) or
            ((readByte().toLong() and 0b1111_1111) shl 8) or
            ((readByte().toLong() and 0b1111_1111) shl 0)

    fun readChar(): Char = (
        ((readByte().toInt() and 0b1111_1111) shl 8) or
            ((readByte().toInt() and 0b1111_1111) shl 0)
        ).toChar()

    fun readFloat(): Float = java.lang.Float.intBitsToFloat(readInt())
    fun readDouble(): Double = java.lang.Double.longBitsToDouble(readLong())
    fun readVarInt(): Int {
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

    fun readZigZagInt(): Int {
        val value = readVarInt()
        return (value ushr 1) xor -(value and 0b0000_0001)
    }

    fun readVarLong(): Long {
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

    fun readZigZagLong(): Long {
        val value = readVarLong()
        return (value ushr 1) xor -(value and 0b0000_0001)
    }

    fun stream() = object : InputStream() {
        override fun read(): Int = readByte().toInt() and 0b1111_1111
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            readBytes(b, off, len)
            return len
        }
    }
}

fun reader(input: InputStream) = object : Reader() {
    override fun readByte(): Byte {
        val i = input.read()
        check(i >= 0) { "end of stream reached" }
        return i.toByte()
    }

    override fun readBytes(buffer: ByteArray, offset: Int, length: Int) {
        // check(input.readNBytes(buffer, offset, length) == length) { "end of stream reached" } $todo: if Java >= 9
        var n = 0
        while (n < length) {
            val count = input.read(buffer, offset + n, length - n)
            check(count >= 0) { "end of stream reached" }
            n += count
        }
    }
}

fun reader(input: ByteBuffer) = object : Reader() {
    override fun readByte() = input.get()
    override fun readBytes(buffer: ByteArray, offset: Int, length: Int) {
        input.get(buffer, offset, length)
    }
}

fun utf8toString(value: ByteArray): String = String(value, StandardCharsets.UTF_8)
