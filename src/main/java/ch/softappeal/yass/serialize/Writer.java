package ch.softappeal.yass.serialize;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;

import java.io.IOException;
import java.io.OutputStream;

public abstract class Writer {

  public abstract void writeByte(byte value) throws Exception;

  public abstract void writeBytes(byte[] buffer, int offset, int length) throws Exception;

  public final void writeBytes(final byte[] buffer) throws Exception {
    writeBytes(buffer, 0, buffer.length);
  }

  /**
   * Writes a short as 2 bytes, high byte first.
   */
  public final void writeShort(final short value) throws Exception {
    writeByte((byte)(value >> 8));
    writeByte((byte)(value >> 0));
  }

  /**
   * Writes an int as 4 bytes, high byte first.
   */
  public final void writeInt(final int value) throws Exception {
    writeByte((byte)(value >> 24));
    writeByte((byte)(value >> 16));
    writeByte((byte)(value >> 8));
    writeByte((byte)(value >> 0));
  }

  /**
   * Writes a long as 8 bytes, high byte first.
   */
  public final void writeLong(final long value) throws Exception {
    writeByte((byte)(value >> 56));
    writeByte((byte)(value >> 48));
    writeByte((byte)(value >> 40));
    writeByte((byte)(value >> 32));
    writeByte((byte)(value >> 24));
    writeByte((byte)(value >> 16));
    writeByte((byte)(value >> 8));
    writeByte((byte)(value >> 0));
  }

  /**
   * Writes a char as 2 bytes, high byte first.
   */
  public final void writeChar(final char value) throws Exception {
    writeByte((byte)(value >> 8));
    writeByte((byte)(value >> 0));
  }

  /**
   * Writes a float as 4 bytes, high byte first.
   */
  public final void writeFloat(final float value) throws Exception {
    writeInt(Float.floatToRawIntBits(value));
  }

  /**
   * Writes a double as 8 bytes, high byte first.
   */
  public final void writeDouble(final double value) throws Exception {
    writeLong(Double.doubleToRawLongBits(value));
  }

  /**
   * Writes an int with <a href="https://developers.google.com/protocol-buffers/docs/encoding#varints">variable length encoding</a>.
   */
  public final void writeVarInt(int value) throws Exception {
    while (true) {
      if ((value & ~0b0111_1111) == 0) {
        writeByte((byte)value);
        return;
      }
      writeByte((byte)((value & 0b0111_1111) | 0b1000_0000));
      value >>>= 7;
    }
  }

  /**
   * Writes a <a href="https://developers.google.com/protocol-buffers/docs/encoding#types">zigzag encoded</a> int with {@link #writeVarInt(int)}.
   */
  public final void writeZigZagInt(final int value) throws Exception {
    writeVarInt((value << 1) ^ (value >> 31));
  }

  /**
   * Writes a long with <a href="https://developers.google.com/protocol-buffers/docs/encoding#varints">variable length encoding</a>.
   */
  public final void writeVarLong(long value) throws Exception {
    while (true) {
      if ((value & ~0b0111_1111) == 0) {
        writeByte((byte)value);
        return;
      }
      writeByte((byte)((value & 0b0111_1111) | 0b1000_0000));
      value >>>= 7;
    }
  }

  /**
   * Writes a <a href="https://developers.google.com/protocol-buffers/docs/encoding#types">zigzag encoded</a> long with {@link #writeVarLong(long)}.
   */
  public final void writeZigZagLong(final long value) throws Exception {
    writeVarLong((value << 1) ^ (value >> 63));
  }

  //  /**
  //   * @return the length in bytes of an UTF-8 encoded {@link CharSequence}
  //   */
  //  public static int calculateUtf8Length(final CharSequence value) {
  //    int utf8Length = 0;
  //    final int length = value.length();
  //    for (int i = 0; i < length; i++) {
  //      final int c = value.charAt(i);
  //      if (c <= 0b0111_1111) {
  //        utf8Length++;
  //      } else if (c <= 0b0111_1111_1111) {
  //        utf8Length += 2;
  //      } else {
  //        utf8Length += 3;
  //      }
  //    }
  //    return utf8Length;
  //  }
  //
  //  /**
  //   * Write a {@link CharSequence} with UTF-8 encoding.
  //   * Note: This method seems to faster than String#getBytes(Charset) for short strings.
  //   * @see #calculateUtf8Length(CharSequence)
  //   */
  //  public final void writeString(final CharSequence value) throws Exception {
  //    final int length = value.length();
  //    for (int i = 0; i < length; i++) {
  //      final int c = value.charAt(i);
  //      if (c <= 0b0111_1111) {
  //        writeByte((byte)c);
  //      } else if (c <= 0b0111_1111_1111) {
  //        writeByte((byte)(0b1100_0000 | ((c >> 6) & 0b0001_1111)));
  //        writeByte((byte)(0b1000_0000 | ((c >> 0) & 0b0011_1111)));
  //      } else {
  //        writeByte((byte)(0b1110_0000 | ((c >> 12) & 0b0000_1111)));
  //        writeByte((byte)(0b1000_0000 | ((c >> 6) & 0b0011_1111)));
  //        writeByte((byte)(0b1000_0000 | ((c >> 0) & 0b0011_1111)));
  //      }
  //    }
  //  }

  public final OutputStream stream() {
    return new OutputStream() {
      @Override public void write(final int i) {
        try {
          writeByte((byte)i);
        } catch (final Exception e) {
          throw Exceptions.wrap(e);
        }
      }
      @Override public void write(final byte[] b, final int off, final int len) {
        try {
          writeBytes(b, off, len);
        } catch (final Exception e) {
          throw Exceptions.wrap(e);
        }
      }
    };
  }

  public static Writer create(final OutputStream out) {
    Check.notNull(out);
    return new Writer() {
      @Override public void writeByte(final byte value) throws IOException {
        out.write(value);
      }
      @Override public void writeBytes(final byte[] buffer, final int offset, final int length) throws IOException {
        out.write(buffer, offset, length);
      }
    };
  }

}
