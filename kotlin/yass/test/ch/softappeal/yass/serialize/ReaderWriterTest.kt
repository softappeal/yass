package ch.softappeal.yass.serialize

import org.junit.jupiter.api.Assertions.assertArrayEquals
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Arrays
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ReaderWriterTest {

    @Test
    fun adaptorByte() {
        val p0 = 0
        val p100 = 100
        val p127 = 127
        val m100 = -100
        val m128 = -128
        val out = ByteArrayOutputStream()
        val writer = writer(out).stream()
        writer.write(p0)
        writer.write(p100)
        writer.write(p127)
        writer.write(m100)
        writer.write(m128)
        val buffer = out.toByteArray()
        assertTrue(buffer[0] == p0.toByte())
        assertTrue(buffer[1] == p100.toByte())
        assertTrue(buffer[2] == p127.toByte())
        assertTrue(buffer[3] == m100.toByte())
        assertTrue(buffer[4] == m128.toByte())
        val reader = reader(ByteArrayInputStream(buffer)).stream()
        assertTrue(reader.read() == (p0 and 0b1111_1111))
        assertTrue(reader.read() == (p100 and 0b1111_1111))
        assertTrue(reader.read() == (p127 and 0b1111_1111))
        assertTrue(reader.read() == (m100 and 0b1111_1111))
        assertTrue(reader.read() == (m128 and 0b1111_1111))
        try {
            reader.read()
            fail()
        } catch (e: IllegalStateException) {
            println(e)
        }
    }

    @Test
    fun adaptorBytes() {
        val input = byteArrayOf(0, 100, 127, -100, -128)
        val out = ByteArrayOutputStream()
        val writer = writer(out).stream()
        writer.write(input, 0, input.size)
        val buffer = out.toByteArray()
        assertArrayEquals(input, buffer)
        val reader = reader(ByteArrayInputStream(buffer)).stream()
        val output = ByteArray(input.size)
        assertTrue(reader.read(output, 0, output.size) == input.size)
        assertArrayEquals(output, input)
        try {
            reader.read()
            fail()
        } catch (e: IllegalStateException) {
            println(e)
        }
    }

    @Test
    fun bytes() {
        val out = ByteArrayOutputStream()
        val writer = writer(out)
        writer.writeBytes(byteArrayOf(-1, 0, 1))
        val reader = reader(ByteArrayInputStream(out.toByteArray()))
        val bytes = ByteArray(3)
        reader.readBytes(bytes)
        assertTrue(Arrays.equals(bytes, byteArrayOf(-1, 0, 1)))
    }

    @Test
    fun numbers() {
        val out = ByteArrayOutputStream()
        val writer = writer(out)
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
        val reader = reader(ByteArrayInputStream(out.toByteArray()))
        assertTrue(reader.readShort() == 12.toShort())
        assertTrue(reader.readShort() == 0.toShort())
        assertTrue(reader.readShort() == (-34).toShort())
        assertTrue(reader.readShort() == 0x1234.toShort())
        assertTrue(reader.readShort() == (-0x124).toShort())
        assertTrue(reader.readShort() == Short.MIN_VALUE)
        assertTrue(reader.readShort() == Short.MAX_VALUE)
        assertTrue(reader.readInt() == 12)
        assertTrue(reader.readInt() == 0)
        assertTrue(reader.readInt() == -34)
        assertTrue(reader.readInt() == 0x1234_5678)
        assertTrue(reader.readInt() == -0x123_4568)
        assertTrue(reader.readInt() == Integer.MIN_VALUE)
        assertTrue(reader.readInt() == Integer.MAX_VALUE)
        assertTrue(reader.readLong() == 12L)
        assertTrue(reader.readLong() == 0L)
        assertTrue(reader.readLong() == -34L)
        assertTrue(reader.readLong() == 0x1234_5678_9ABC_DEF0)
        assertTrue(reader.readLong() == -0x123_4567_89ab_cdf0)
        assertTrue(reader.readLong() == Long.MIN_VALUE)
        assertTrue(reader.readLong() == Long.MAX_VALUE)
        assertTrue(reader.readChar() == '\u1234')
        assertTrue(reader.readChar() == '\uFEDC')
        assertTrue(reader.readChar() == Character.MIN_VALUE)
        assertTrue(reader.readChar() == Character.MAX_VALUE)
        assertTrue(reader.readFloat() == 1.2345e-12f)
        assertTrue(reader.readFloat() == Float.MAX_VALUE)
        assertTrue(reader.readFloat() == Float.MIN_VALUE)
        assertTrue(reader.readFloat() == java.lang.Float.MIN_NORMAL)
        assertTrue(reader.readFloat() == Float.NEGATIVE_INFINITY)
        assertTrue(reader.readFloat() == Float.POSITIVE_INFINITY)
        assertEquals("NaN", reader.readFloat().toString())
        assertTrue(reader.readDouble() == 1.2345e-12)
        assertTrue(reader.readDouble() == Double.MAX_VALUE)
        assertTrue(reader.readDouble() == Double.MIN_VALUE)
        assertTrue(reader.readDouble() == java.lang.Double.MIN_NORMAL)
        assertTrue(reader.readDouble() == Double.NEGATIVE_INFINITY)
        assertTrue(reader.readDouble() == Double.POSITIVE_INFINITY)
        assertEquals("NaN", reader.readDouble().toString())
    }

    @Test
    fun varInt() {
        val out = ByteArrayOutputStream()
        val writer = writer(out)
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
        val reader = reader(ByteArrayInputStream(out.toByteArray()))
        assertTrue(reader.readVarInt() == 12)
        assertTrue(reader.readVarInt() == 0)
        assertTrue(reader.readVarInt() == 128)
        assertTrue(reader.readVarInt() == 60000)
        assertTrue(reader.readVarInt() == 60000000)
        assertTrue(reader.readVarInt() == -34)
        assertTrue(reader.readVarInt() == 0x12345678)
        assertTrue(reader.readVarInt() == -0x1234568)
        assertTrue(reader.readVarInt() == Integer.MIN_VALUE)
        assertTrue(reader.readVarInt() == Integer.MAX_VALUE)
        assertTrue(reader.readZigZagInt() == 12)
        assertTrue(reader.readZigZagInt() == 0)
        assertTrue(reader.readZigZagInt() == 128)
        assertTrue(reader.readZigZagInt() == 60000)
        assertTrue(reader.readZigZagInt() == 60000000)
        assertTrue(reader.readZigZagInt() == -34)
        assertTrue(reader.readZigZagInt() == 0x12345678)
        assertTrue(reader.readZigZagInt() == -0x1234568)
        assertTrue(reader.readZigZagInt() == Integer.MIN_VALUE)
        assertTrue(reader.readZigZagInt() == Integer.MAX_VALUE)
    }

    @Test
    fun varLong() {
        val out = ByteArrayOutputStream()
        val writer = writer(out)
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
        val reader = reader(ByteArrayInputStream(out.toByteArray()))
        assertTrue(reader.readVarLong() == 12L)
        assertTrue(reader.readVarLong() == 0L)
        assertTrue(reader.readVarLong() == 128L)
        assertTrue(reader.readVarLong() == -34L)
        assertTrue(reader.readVarLong() == 0x123456789ABCDEF0)
        assertTrue(reader.readVarLong() == -0x123456789abcdf0)
        assertTrue(reader.readVarLong() == Long.MIN_VALUE)
        assertTrue(reader.readVarLong() == Long.MAX_VALUE)
        assertTrue(reader.readZigZagLong() == 12L)
        assertTrue(reader.readZigZagLong() == 0L)
        assertTrue(reader.readZigZagLong() == 128L)
        assertTrue(reader.readZigZagLong() == -34L)
        assertTrue(reader.readZigZagLong() == 0x123456789ABCDEF0)
        assertTrue(reader.readZigZagLong() == -0x123456789abcdf0)
        assertTrue(reader.readZigZagLong() == Long.MIN_VALUE)
        assertTrue(reader.readZigZagLong() == Long.MAX_VALUE)
    }

    private fun string(utf8Length: Int, value: String) {
        val bytes = utf8toBytes(value)
        assertTrue(bytes.size == utf8Length)
        assertEquals(value, utf8toString(bytes))
    }

    @Test
    fun string() {
        string(2, "><")
        string(3, ">\u0000<")
        string(3, ">\u0001<")
        string(3, ">\u0012<")
        string(3, ">\u007F<")
        string(4, ">\u0080<")
        string(4, ">\u0234<")
        string(4, ">\u07FF<")
        string(5, ">\u0800<")
        string(5, ">\u4321<")
        string(5, ">\uFFFF<")
    }

}
