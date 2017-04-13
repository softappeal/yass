package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Utf8;
import ch.softappeal.yass.serialize.Writer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class ReaderWriterTest {

    @Test public void adaptorByte() throws IOException {
        final byte p0 = 0;
        final byte p100 = 100;
        final byte p127 = 127;
        final byte m100 = -100;
        final byte m128 = -128;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final OutputStream writer = Writer.create(out).stream();
        writer.write(p0);
        writer.write(p100);
        writer.write(p127);
        writer.write(m100);
        writer.write(m128);
        final byte[] buffer = out.toByteArray();
        Assert.assertTrue(buffer[0] == p0);
        Assert.assertTrue(buffer[1] == p100);
        Assert.assertTrue(buffer[2] == p127);
        Assert.assertTrue(buffer[3] == m100);
        Assert.assertTrue(buffer[4] == m128);
        final InputStream reader = Reader.create(new ByteArrayInputStream(buffer)).stream();
        Assert.assertTrue(reader.read() == (p0 & 0b1111_1111));
        Assert.assertTrue(reader.read() == (p100 & 0b1111_1111));
        Assert.assertTrue(reader.read() == (p127 & 0b1111_1111));
        Assert.assertTrue(reader.read() == (m100 & 0b1111_1111));
        Assert.assertTrue(reader.read() == (m128 & 0b1111_1111));
        try {
            reader.read();
            Assert.fail();
        } catch (final RuntimeException e) {
            System.out.println(e);
        }
    }

    @Test public void adaptorBytes() throws IOException {
        final byte[] input = {0, 100, 127, -100, -128};
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final OutputStream writer = Writer.create(out).stream();
        writer.write(input, 0, input.length);
        final byte[] buffer = out.toByteArray();
        Assert.assertArrayEquals(input, buffer);
        final InputStream reader = Reader.create(new ByteArrayInputStream(buffer)).stream();
        final byte[] output = new byte[input.length];
        Assert.assertTrue(reader.read(output, 0, output.length) == input.length);
        Assert.assertArrayEquals(output, input);
        try {
            reader.read();
            Assert.fail();
        } catch (final RuntimeException e) {
            System.out.println(e);
        }
    }

    @Test public void bytes() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = Writer.create(out);
        writer.writeBytes(new byte[] {(byte)-1, (byte)0, (byte)1});
        final Reader reader = Reader.create(new ByteArrayInputStream(out.toByteArray()));
        final byte[] bytes = new byte[3];
        reader.readBytes(bytes);
        Assert.assertTrue(Arrays.equals(bytes, new byte[] {(byte)-1, (byte)0, (byte)1}));
    }

    @Test public void numbers() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = Writer.create(out);
        writer.writeShort((short)12);
        writer.writeShort((short)0);
        writer.writeShort((short)-34);
        writer.writeShort((short)0x1234);
        writer.writeShort((short)0xFEDC);
        writer.writeShort(Short.MIN_VALUE);
        writer.writeShort(Short.MAX_VALUE);
        writer.writeInt(12);
        writer.writeInt(0);
        writer.writeInt(-34);
        writer.writeInt(0x12345678);
        writer.writeInt(0xFEDCBA98);
        writer.writeInt(Integer.MIN_VALUE);
        writer.writeInt(Integer.MAX_VALUE);
        writer.writeLong(12);
        writer.writeLong(0);
        writer.writeLong(-34);
        writer.writeLong(0x123456789ABCDEF0L);
        writer.writeLong(0xFEDCBA9876543210L);
        writer.writeLong(Long.MIN_VALUE);
        writer.writeLong(Long.MAX_VALUE);
        writer.writeChar('\u1234');
        writer.writeChar('\uFEDC');
        writer.writeChar(Character.MIN_VALUE);
        writer.writeChar(Character.MAX_VALUE);
        writer.writeFloat(1.2345e-12f);
        writer.writeFloat(Float.MAX_VALUE);
        writer.writeFloat(Float.MIN_VALUE);
        writer.writeFloat(Float.MIN_NORMAL);
        writer.writeFloat(Float.NEGATIVE_INFINITY);
        writer.writeFloat(Float.POSITIVE_INFINITY);
        writer.writeFloat(Float.NaN);
        writer.writeDouble(1.2345e-12d);
        writer.writeDouble(Double.MAX_VALUE);
        writer.writeDouble(Double.MIN_VALUE);
        writer.writeDouble(Double.MIN_NORMAL);
        writer.writeDouble(Double.NEGATIVE_INFINITY);
        writer.writeDouble(Double.POSITIVE_INFINITY);
        writer.writeDouble(Double.NaN);
        final Reader reader = Reader.create(new ByteArrayInputStream(out.toByteArray()));
        Assert.assertTrue(reader.readShort() == 12);
        Assert.assertTrue(reader.readShort() == 0);
        Assert.assertTrue(reader.readShort() == -34);
        Assert.assertTrue(reader.readShort() == 0x1234);
        Assert.assertTrue(reader.readShort() == 0xFFFFFEDC);
        Assert.assertTrue(reader.readShort() == Short.MIN_VALUE);
        Assert.assertTrue(reader.readShort() == Short.MAX_VALUE);
        Assert.assertTrue(reader.readInt() == 12);
        Assert.assertTrue(reader.readInt() == 0);
        Assert.assertTrue(reader.readInt() == -34);
        Assert.assertTrue(reader.readInt() == 0x12345678);
        Assert.assertTrue(reader.readInt() == 0xFEDCBA98);
        Assert.assertTrue(reader.readInt() == Integer.MIN_VALUE);
        Assert.assertTrue(reader.readInt() == Integer.MAX_VALUE);
        Assert.assertTrue(reader.readLong() == 12);
        Assert.assertTrue(reader.readLong() == 0);
        Assert.assertTrue(reader.readLong() == -34);
        Assert.assertTrue(reader.readLong() == 0x123456789ABCDEF0L);
        Assert.assertTrue(reader.readLong() == 0xFEDCBA9876543210L);
        Assert.assertTrue(reader.readLong() == Long.MIN_VALUE);
        Assert.assertTrue(reader.readLong() == Long.MAX_VALUE);
        Assert.assertTrue(reader.readChar() == '\u1234');
        Assert.assertTrue(reader.readChar() == '\uFEDC');
        Assert.assertTrue(reader.readChar() == Character.MIN_VALUE);
        Assert.assertTrue(reader.readChar() == Character.MAX_VALUE);
        Assert.assertTrue(reader.readFloat() == 1.2345e-12f);
        Assert.assertTrue(reader.readFloat() == Float.MAX_VALUE);
        Assert.assertTrue(reader.readFloat() == Float.MIN_VALUE);
        Assert.assertTrue(reader.readFloat() == Float.MIN_NORMAL);
        Assert.assertTrue(reader.readFloat() == Float.NEGATIVE_INFINITY);
        Assert.assertTrue(reader.readFloat() == Float.POSITIVE_INFINITY);
        Assert.assertEquals("NaN", String.valueOf(reader.readFloat()));
        Assert.assertTrue(reader.readDouble() == 1.2345e-12d);
        Assert.assertTrue(reader.readDouble() == Double.MAX_VALUE);
        Assert.assertTrue(reader.readDouble() == Double.MIN_VALUE);
        Assert.assertTrue(reader.readDouble() == Double.MIN_NORMAL);
        Assert.assertTrue(reader.readDouble() == Double.NEGATIVE_INFINITY);
        Assert.assertTrue(reader.readDouble() == Double.POSITIVE_INFINITY);
        Assert.assertEquals("NaN", String.valueOf(reader.readDouble()));
    }

    @Test public void varInt() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = Writer.create(out);
        writer.writeVarInt(12);
        writer.writeVarInt(0);
        writer.writeVarInt(128);
        writer.writeVarInt(60000);
        writer.writeVarInt(60000000);
        writer.writeVarInt(-34);
        writer.writeVarInt(0x12345678);
        writer.writeVarInt(0xFEDCBA98);
        writer.writeVarInt(Integer.MIN_VALUE);
        writer.writeVarInt(Integer.MAX_VALUE);
        writer.writeZigZagInt(12);
        writer.writeZigZagInt(0);
        writer.writeZigZagInt(128);
        writer.writeZigZagInt(60000);
        writer.writeZigZagInt(60000000);
        writer.writeZigZagInt(-34);
        writer.writeZigZagInt(0x12345678);
        writer.writeZigZagInt(0xFEDCBA98);
        writer.writeZigZagInt(Integer.MIN_VALUE);
        writer.writeZigZagInt(Integer.MAX_VALUE);
        final Reader reader = Reader.create(new ByteArrayInputStream(out.toByteArray()));
        Assert.assertTrue(reader.readVarInt() == 12);
        Assert.assertTrue(reader.readVarInt() == 0);
        Assert.assertTrue(reader.readVarInt() == 128);
        Assert.assertTrue(reader.readVarInt() == 60000);
        Assert.assertTrue(reader.readVarInt() == 60000000);
        Assert.assertTrue(reader.readVarInt() == -34);
        Assert.assertTrue(reader.readVarInt() == 0x12345678);
        Assert.assertTrue(reader.readVarInt() == 0xFEDCBA98);
        Assert.assertTrue(reader.readVarInt() == Integer.MIN_VALUE);
        Assert.assertTrue(reader.readVarInt() == Integer.MAX_VALUE);
        Assert.assertTrue(reader.readZigZagInt() == 12);
        Assert.assertTrue(reader.readZigZagInt() == 0);
        Assert.assertTrue(reader.readZigZagInt() == 128);
        Assert.assertTrue(reader.readZigZagInt() == 60000);
        Assert.assertTrue(reader.readZigZagInt() == 60000000);
        Assert.assertTrue(reader.readZigZagInt() == -34);
        Assert.assertTrue(reader.readZigZagInt() == 0x12345678);
        Assert.assertTrue(reader.readZigZagInt() == 0xFEDCBA98);
        Assert.assertTrue(reader.readZigZagInt() == Integer.MIN_VALUE);
        Assert.assertTrue(reader.readZigZagInt() == Integer.MAX_VALUE);
    }

    @Test public void varLong() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = Writer.create(out);
        writer.writeVarLong(12);
        writer.writeVarLong(0);
        writer.writeVarLong(128);
        writer.writeVarLong(-34);
        writer.writeVarLong(0x123456789ABCDEF0L);
        writer.writeVarLong(0xFEDCBA9876543210L);
        writer.writeVarLong(Long.MIN_VALUE);
        writer.writeVarLong(Long.MAX_VALUE);
        writer.writeZigZagLong(12);
        writer.writeZigZagLong(0);
        writer.writeZigZagLong(128);
        writer.writeZigZagLong(-34);
        writer.writeZigZagLong(0x123456789ABCDEF0L);
        writer.writeZigZagLong(0xFEDCBA9876543210L);
        writer.writeZigZagLong(Long.MIN_VALUE);
        writer.writeZigZagLong(Long.MAX_VALUE);
        final Reader reader = Reader.create(new ByteArrayInputStream(out.toByteArray()));
        Assert.assertTrue(reader.readVarLong() == 12);
        Assert.assertTrue(reader.readVarLong() == 0);
        Assert.assertTrue(reader.readVarLong() == 128);
        Assert.assertTrue(reader.readVarLong() == -34);
        Assert.assertTrue(reader.readVarLong() == 0x123456789ABCDEF0L);
        Assert.assertTrue(reader.readVarLong() == 0xFEDCBA9876543210L);
        Assert.assertTrue(reader.readVarLong() == Long.MIN_VALUE);
        Assert.assertTrue(reader.readVarLong() == Long.MAX_VALUE);
        Assert.assertTrue(reader.readZigZagLong() == 12);
        Assert.assertTrue(reader.readZigZagLong() == 0);
        Assert.assertTrue(reader.readZigZagLong() == 128);
        Assert.assertTrue(reader.readZigZagLong() == -34);
        Assert.assertTrue(reader.readZigZagLong() == 0x123456789ABCDEF0L);
        Assert.assertTrue(reader.readZigZagLong() == 0xFEDCBA9876543210L);
        Assert.assertTrue(reader.readZigZagLong() == Long.MIN_VALUE);
        Assert.assertTrue(reader.readZigZagLong() == Long.MAX_VALUE);
    }

    private static void string(final int utf8Length, final String value) {
        final byte[] bytes = Utf8.bytes(value);
        Assert.assertTrue(bytes.length == utf8Length);
        Assert.assertEquals(value, Utf8.string(bytes));
    }

    @Test public void string() {
        string(2, "><");
        string(3, ">\u0000<");
        string(3, ">\u0001<");
        string(3, ">\u0012<");
        string(3, ">\u007F<");
        string(4, ">\u0080<");
        string(4, ">\u0234<");
        string(4, ">\u07FF<");
        string(5, ">\u0800<");
        string(5, ">\u4321<");
        string(5, ">\uFFFF<");
    }

}
