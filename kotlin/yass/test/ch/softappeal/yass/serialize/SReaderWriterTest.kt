package ch.softappeal.yass.serialize

import kotlinx.coroutines.*
import java.io.*
import java.util.*
import kotlin.test.*

private fun SWriter.stream() = object : OutputStream() {
    override fun write(i: Int) = runBlocking { writeByte(i.toByte()) }
    override fun write(b: ByteArray, off: Int, len: Int) = runBlocking { writeBytes(b, off, len) }
}

private fun SReader.stream() = object : InputStream() {
    override fun read(): Int = runBlocking { readByte().toInt() and 0b1111_1111 }
    override fun read(b: ByteArray, off: Int, len: Int): Int = runBlocking {
        readBytes(b, off, len)
        len
    }
}

class SReaderWriterTest {
    @Test
    fun adaptorByte() {
        val p0 = 0
        val p100 = 100
        val p127 = 127
        val m100 = -100
        val m128 = -128
        val out = ByteArrayOutputStream()
        val writer = sWriter(out).stream()
        writer.write(p0)
        writer.write(p100)
        writer.write(p127)
        writer.write(m100)
        writer.write(m128)
        val buffer = out.toByteArray()
        assertEquals(p0.toByte(), buffer[0])
        assertEquals(p100.toByte(), buffer[1])
        assertEquals(p127.toByte(), buffer[2])
        assertEquals(m100.toByte(), buffer[3])
        assertEquals(m128.toByte(), buffer[4])
        val reader = sReader(ByteArrayInputStream(buffer)).stream()
        assertEquals((p0 and 0b1111_1111), reader.read())
        assertEquals((p100 and 0b1111_1111), reader.read())
        assertEquals((p127 and 0b1111_1111), reader.read())
        assertEquals((m100 and 0b1111_1111), reader.read())
        assertEquals((m128 and 0b1111_1111), reader.read())
        assertEquals(
            "end of stream reached",
            assertFailsWith<IllegalStateException> { reader.read() }.message
        )
    }

    @Test
    fun adaptorBytes() {
        val input = byteArrayOf(0, 100, 127, -100, -128)
        val out = ByteArrayOutputStream()
        val writer = sWriter(out).stream()
        writer.write(input, 0, input.size)
        val buffer = out.toByteArray()
        assertTrue(Arrays.equals(input, buffer))
        val reader = sReader(ByteArrayInputStream(buffer)).stream()
        val output = ByteArray(input.size)
        assertEquals(input.size, reader.read(output, 0, output.size))
        assertTrue(Arrays.equals(output, input))
        assertEquals(
            "end of stream reached",
            assertFailsWith<IllegalStateException> { reader.read() }.message
        )
    }

    @Test
    fun bytes() = runBlocking {
        val out = ByteArrayOutputStream()
        val writer = sWriter(out)
        writer.writeBytes(byteArrayOf(-1, 0, 1))
        val reader = sReader(ByteArrayInputStream(out.toByteArray()))
        val bytes = ByteArray(3)
        reader.readBytes(bytes)
        assertTrue(Arrays.equals(bytes, byteArrayOf(-1, 0, 1)))
    }

    @Test
    fun numbers() = runBlocking {
        val out = ByteArrayOutputStream()
        val writer = sWriter(out)
        writer.writeShort(12.toShort())
        writer.writeShort(0.toShort())
        writer.writeShort((-34).toShort())
        writer.writeShort(0x1234.toShort())
        writer.writeShort(0xFEDC.toShort())
        writer.writeShort(Short.MIN_VALUE)
        writer.writeShort(Short.MAX_VALUE)
        writer.writeInt(12)
        writer.writeInt(0)
        writer.writeInt(-34)
        writer.writeInt(0x1234_5678)
        writer.writeInt(-0x123_4568)
        writer.writeInt(Integer.MIN_VALUE)
        writer.writeInt(Integer.MAX_VALUE)
        writer.writeLong(12)
        writer.writeLong(0)
        writer.writeLong(-34)
        writer.writeLong(0x1234_5678_9ABC_DEF0)
        writer.writeLong(-0x123_4567_89ab_cdf0)
        writer.writeLong(Long.MIN_VALUE)
        writer.writeLong(Long.MAX_VALUE)
        writer.writeChar('\u1234')
        writer.writeChar('\uFEDC')
        writer.writeChar(Character.MIN_VALUE)
        writer.writeChar(Character.MAX_VALUE)
        writer.writeFloat(1.2345e-12f)
        writer.writeFloat(Float.MAX_VALUE)
        writer.writeFloat(Float.MIN_VALUE)
        writer.writeFloat(java.lang.Float.MIN_NORMAL)
        writer.writeFloat(Float.NEGATIVE_INFINITY)
        writer.writeFloat(Float.POSITIVE_INFINITY)
        writer.writeFloat(Float.NaN)
        writer.writeDouble(1.2345e-12)
        writer.writeDouble(Double.MAX_VALUE)
        writer.writeDouble(Double.MIN_VALUE)
        writer.writeDouble(java.lang.Double.MIN_NORMAL)
        writer.writeDouble(Double.NEGATIVE_INFINITY)
        writer.writeDouble(Double.POSITIVE_INFINITY)
        writer.writeDouble(Double.NaN)
        val reader = sReader(ByteArrayInputStream(out.toByteArray()))
        assertEquals(12.toShort(), reader.readShort())
        assertEquals(0.toShort(), reader.readShort())
        assertEquals((-34).toShort(), reader.readShort())
        assertEquals(0x1234.toShort(), reader.readShort())
        assertEquals((-0x124).toShort(), reader.readShort())
        assertEquals(Short.MIN_VALUE, reader.readShort())
        assertEquals(Short.MAX_VALUE, reader.readShort())
        assertEquals(12, reader.readInt())
        assertEquals(0, reader.readInt())
        assertEquals(-34, reader.readInt())
        assertEquals(0x1234_5678, reader.readInt())
        assertEquals(-0x123_4568, reader.readInt())
        assertEquals(Integer.MIN_VALUE, reader.readInt())
        assertEquals(Integer.MAX_VALUE, reader.readInt())
        assertEquals(12L, reader.readLong())
        assertEquals(0L, reader.readLong())
        assertEquals(-34L, reader.readLong())
        assertEquals(0x1234_5678_9ABC_DEF0, reader.readLong())
        assertEquals(-0x123_4567_89ab_cdf0, reader.readLong())
        assertEquals(Long.MIN_VALUE, reader.readLong())
        assertEquals(Long.MAX_VALUE, reader.readLong())
        assertEquals('\u1234', reader.readChar())
        assertEquals('\uFEDC', reader.readChar())
        assertEquals(Character.MIN_VALUE, reader.readChar())
        assertEquals(Character.MAX_VALUE, reader.readChar())
        assertEquals(1.2345e-12f, reader.readFloat())
        assertEquals(Float.MAX_VALUE, reader.readFloat())
        assertEquals(Float.MIN_VALUE, reader.readFloat())
        assertEquals(java.lang.Float.MIN_NORMAL, reader.readFloat())
        assertEquals(Float.NEGATIVE_INFINITY, reader.readFloat())
        assertEquals(Float.POSITIVE_INFINITY, reader.readFloat())
        assertEquals("NaN", reader.readFloat().toString())
        assertEquals(1.2345e-12, reader.readDouble())
        assertEquals(Double.MAX_VALUE, reader.readDouble())
        assertEquals(Double.MIN_VALUE, reader.readDouble())
        assertEquals(java.lang.Double.MIN_NORMAL, reader.readDouble())
        assertEquals(Double.NEGATIVE_INFINITY, reader.readDouble())
        assertEquals(Double.POSITIVE_INFINITY, reader.readDouble())
        assertEquals("NaN", reader.readDouble().toString())
    }

    @Test
    fun varInt() = runBlocking {
        val out = ByteArrayOutputStream()
        val writer = sWriter(out)
        writer.writeVarInt(12)
        writer.writeVarInt(0)
        writer.writeVarInt(128)
        writer.writeVarInt(60000)
        writer.writeVarInt(60000000)
        writer.writeVarInt(-34)
        writer.writeVarInt(0x12345678)
        writer.writeVarInt(-0x1234568)
        writer.writeVarInt(Integer.MIN_VALUE)
        writer.writeVarInt(Integer.MAX_VALUE)
        writer.writeZigZagInt(12)
        writer.writeZigZagInt(0)
        writer.writeZigZagInt(128)
        writer.writeZigZagInt(60000)
        writer.writeZigZagInt(60000000)
        writer.writeZigZagInt(-34)
        writer.writeZigZagInt(0x12345678)
        writer.writeZigZagInt(-0x1234568)
        writer.writeZigZagInt(Integer.MIN_VALUE)
        writer.writeZigZagInt(Integer.MAX_VALUE)
        val reader = sReader(ByteArrayInputStream(out.toByteArray()))
        assertEquals(12, reader.readVarInt())
        assertEquals(0, reader.readVarInt())
        assertEquals(128, reader.readVarInt())
        assertEquals(60000, reader.readVarInt())
        assertEquals(60000000, reader.readVarInt())
        assertEquals(-34, reader.readVarInt())
        assertEquals(0x12345678, reader.readVarInt())
        assertEquals(-0x1234568, reader.readVarInt())
        assertEquals(Integer.MIN_VALUE, reader.readVarInt())
        assertEquals(Integer.MAX_VALUE, reader.readVarInt())
        assertEquals(12, reader.readZigZagInt())
        assertEquals(0, reader.readZigZagInt())
        assertEquals(128, reader.readZigZagInt())
        assertEquals(60000, reader.readZigZagInt())
        assertEquals(60000000, reader.readZigZagInt())
        assertEquals(-34, reader.readZigZagInt())
        assertEquals(0x12345678, reader.readZigZagInt())
        assertEquals(-0x1234568, reader.readZigZagInt())
        assertEquals(Integer.MIN_VALUE, reader.readZigZagInt())
        assertEquals(Integer.MAX_VALUE, reader.readZigZagInt())
    }

    @Test
    fun varLong() = runBlocking {
        val out = ByteArrayOutputStream()
        val writer = sWriter(out)
        writer.writeVarLong(12)
        writer.writeVarLong(0)
        writer.writeVarLong(128)
        writer.writeVarLong(-34)
        writer.writeVarLong(0x123456789ABCDEF0)
        writer.writeVarLong(-0x123456789abcdf0)
        writer.writeVarLong(Long.MIN_VALUE)
        writer.writeVarLong(Long.MAX_VALUE)
        writer.writeZigZagLong(12)
        writer.writeZigZagLong(0)
        writer.writeZigZagLong(128)
        writer.writeZigZagLong(-34)
        writer.writeZigZagLong(0x123456789ABCDEF0)
        writer.writeZigZagLong(-0x123456789abcdf0)
        writer.writeZigZagLong(Long.MIN_VALUE)
        writer.writeZigZagLong(Long.MAX_VALUE)
        val reader = sReader(ByteArrayInputStream(out.toByteArray()))
        assertEquals(12L, reader.readVarLong())
        assertEquals(0L, reader.readVarLong())
        assertEquals(128L, reader.readVarLong())
        assertEquals(-34L, reader.readVarLong())
        assertEquals(0x123456789ABCDEF0, reader.readVarLong())
        assertEquals(-0x123456789abcdf0, reader.readVarLong())
        assertEquals(Long.MIN_VALUE, reader.readVarLong())
        assertEquals(Long.MAX_VALUE, reader.readVarLong())
        assertEquals(12L, reader.readZigZagLong())
        assertEquals(0L, reader.readZigZagLong())
        assertEquals(128L, reader.readZigZagLong())
        assertEquals(-34L, reader.readZigZagLong())
        assertEquals(0x123456789ABCDEF0, reader.readZigZagLong())
        assertEquals(-0x123456789abcdf0, reader.readZigZagLong())
        assertEquals(Long.MIN_VALUE, reader.readZigZagLong())
        assertEquals(Long.MAX_VALUE, reader.readZigZagLong())
    }
}
