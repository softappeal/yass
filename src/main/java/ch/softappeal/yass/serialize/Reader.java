package ch.softappeal.yass.serialize;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public abstract class Reader {

  public abstract byte readByte() throws Exception;

  public abstract void readBytes(byte[] buffer, int offset, int length) throws Exception;

  public final void readBytes(final byte[] buffer) throws Exception {
    readBytes(buffer, 0, buffer.length);
  }

  /**
   * @see Writer#writeShort(short)
   */
  public final short readShort() throws Exception {
    //noinspection PointlessBitwiseExpression,NumericCastThatLosesPrecision
    return (short)(
      ((readByte() & 0b1111_1111) << 8) |
      ((readByte() & 0b1111_1111) << 0)
    );
  }

  /**
   * @see Writer#writeInt(int)
   */
  public final int readInt() throws Exception {
    //noinspection PointlessBitwiseExpression
    return
      ((readByte() & 0b1111_1111) << 24) |
      ((readByte() & 0b1111_1111) << 16) |
      ((readByte() & 0b1111_1111) << 8) |
      ((readByte() & 0b1111_1111) << 0);
  }

  /**
   * @see Writer#writeLong(long)
   */
  public final long readLong() throws Exception {
    //noinspection PointlessBitwiseExpression
    return
      ((readByte() & 0b1111_1111L) << 56) |
      ((readByte() & 0b1111_1111L) << 48) |
      ((readByte() & 0b1111_1111L) << 40) |
      ((readByte() & 0b1111_1111L) << 32) |
      ((readByte() & 0b1111_1111L) << 24) |
      ((readByte() & 0b1111_1111L) << 16) |
      ((readByte() & 0b1111_1111L) << 8) |
      ((readByte() & 0b1111_1111L) << 0);
  }

  /**
   * @see Writer#writeChar(char)
   */
  public final char readChar() throws Exception {
    //noinspection PointlessBitwiseExpression,NumericCastThatLosesPrecision
    return (char)(
      ((readByte() & 0b1111_1111) << 8) |
      ((readByte() & 0b1111_1111) << 0)
    );
  }

  /**
   * @see Writer#writeFloat(float)
   */
  public final float readFloat() throws Exception {
    return Float.intBitsToFloat(readInt());
  }

  /**
   * @see Writer#writeDouble(double)
   */
  public final double readDouble() throws Exception {
    return Double.longBitsToDouble(readLong());
  }

  /**
   * @see Writer#writeVarInt(int)
   */
  @SuppressWarnings("NestedAssignment")
  public final int readVarInt() throws Exception {
    byte b = readByte();
    if (b >= 0) {
      return b;
    }
    int value = b & 0b0111_1111;
    if ((b = readByte()) >= 0) {
      value |= b << 7;
    } else {
      value |= (b & 0b0111_1111) << 7;
      if ((b = readByte()) >= 0) {
        value |= b << 14;
      } else {
        value |= (b & 0b0111_1111) << 14;
        if ((b = readByte()) >= 0) {
          value |= b << 21;
        } else {
          value |= (b & 0b0111_1111) << 21;
          value |= (b = readByte()) << 28;
          if (b < 0) {
            throw new RuntimeException("malformed input");
          }
        }
      }
    }
    return value;
  }

  /**
   * @see Writer#writeZigZagInt(int)
   */
  public final int readZigZagInt() throws Exception {
    final int value = readVarInt();
    return (value >>> 1) ^ -(value & 1);
  }

  /**
   * @see Writer#writeVarLong(long)
   */
  public final long readVarLong() throws Exception {
    int shift = 0;
    long value = 0;
    while (shift < 64) {
      final byte b = readByte();
      value |= (long)(b & 0b0111_1111) << shift;
      if ((b & 0b1000_0000) == 0) {
        return value;
      }
      shift += 7;
    }
    throw new RuntimeException("malformed input");
  }

  /**
   * @see Writer#writeZigZagLong(long)
   */
  public final long readZigZagLong() throws Exception {
    final long value = readVarLong();
    return (value >>> 1) ^ -(value & 1);
  }

  public final InputStream stream() {
    return new InputStream() {
      @Override public int read() {
        try {
          return readByte() & 0b1111_1111;
        } catch (final Exception e) {
          throw Exceptions.wrap(e);
        }
      }
      @Override public int read(final byte[] b, final int off, final int len) {
        try {
          readBytes(b, off, len);
          return len;
        } catch (final Exception e) {
          throw Exceptions.wrap(e);
        }
      }
    };
  }

  public static Reader create(final InputStream in) {
    Check.notNull(in);
    return new Reader() {
      @Override public byte readByte() throws IOException {
        final int i = in.read();
        if (i < 0) {
          throw new EOFException();
        }
        //noinspection NumericCastThatLosesPrecision
        return (byte)i;
      }
      @Override public void readBytes(final byte[] buffer, final int offset, final int length) throws IOException {
        int n = 0;
        while (n < length) {
          final int count = in.read(buffer, offset + n, length - n);
          if (count < 0) {
            throw new EOFException();
          }
          n += count;
        }
      }
    };
  }

}
