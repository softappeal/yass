package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Utf8;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class TypeHandlers {

  private TypeHandlers() {
    // disable
  }

  static final TypeHandler NULL = new TypeHandler(Void.class, 0) {
    @Override @Nullable Object readNoId(final Input input) {
      return null;
    }
    @Override void writeNoId(final Object value, final Output output) {
      // empty
    }
  };

  static final TypeHandler REFERENCE = new TypeHandler(Reference.class, 1) {
    @Override Object readNoId(final Input input) throws Exception {
      return input.referenceableObjects.get(input.reader.readVarInt());
    }
    @Override void writeNoId(final Object value, final Output output) throws Exception {
      output.writer.writeVarInt((Integer)value);
    }
  };

  static final TypeHandler LIST = new TypeHandler(List.class, 2) {
    @Override Object readNoId(final Input input) throws Exception {
      int length = input.reader.readVarInt();
      final List<Object> list = new ArrayList<>(Math.min(length, 256)); // note: prevents out-of-memory attack
      while (length-- > 0) {
        list.add(input.readWithId());
      }
      return list;
    }
    @SuppressWarnings("unchecked")
    @Override void writeNoId(final Object value, final Output output) throws Exception {
      final List<Object> list = (List<Object>)value;
      output.writer.writeVarInt(list.size());
      for (final Object e : list) {
        output.writeWithId(e);
      }
    }
  };

  private static final TypeHandler BOOLEAN = new BaseTypeHandler<Boolean>(Boolean.class, 3) {
    @Override Boolean read(final Reader reader) throws Exception {
      return reader.readByte() != 0;
    }
    @Override void write(final Boolean value, final Writer writer) throws Exception {
      writer.writeByte(value ? (byte)1 : (byte)0);
    }
  };

  private static final TypeHandler BYTE = new BaseTypeHandler<Byte>(Byte.class, 4) {
    @Override Byte read(final Reader reader) throws Exception {
      return reader.readByte();
    }
    @Override void write(final Byte value, final Writer writer) throws Exception {
      writer.writeByte(value);
    }
  };

  private static final TypeHandler SHORT = new BaseTypeHandler<Short>(Short.class, 5) {
    @Override Short read(final Reader reader) throws Exception {
      //noinspection NumericCastThatLosesPrecision
      return (short)reader.readZigZagInt();
    }
    @Override void write(final Short value, final Writer writer) throws Exception {
      writer.writeZigZagInt(value);
    }
  };

  static final TypeHandler INTEGER = new BaseTypeHandler<Integer>(Integer.class, 6) {
    @Override Integer read(final Reader reader) throws Exception {
      return reader.readZigZagInt();
    }
    @Override void write(final Integer value, final Writer writer) throws Exception {
      writer.writeZigZagInt(value);
    }
  };

  static final TypeHandler LONG = new BaseTypeHandler<Long>(Long.class, 7) {
    @Override Long read(final Reader reader) throws Exception {
      return reader.readZigZagLong();
    }
    @Override void write(final Long value, final Writer writer) throws Exception {
      writer.writeZigZagLong(value);
    }
  };

  private static final TypeHandler CHARACTER = new BaseTypeHandler<Character>(Character.class, 8) {
    @Override Character read(final Reader reader) throws Exception {
      return reader.readChar();
    }
    @Override void write(final Character value, final Writer writer) throws Exception {
      writer.writeChar(value);
    }
  };

  private static final TypeHandler FLOAT = new BaseTypeHandler<Float>(Float.class, 9) {
    @Override Float read(final Reader reader) throws Exception {
      return reader.readFloat();
    }
    @Override void write(final Float value, final Writer writer) throws Exception {
      writer.writeFloat(value);
    }
  };

  private static final TypeHandler DOUBLE = new BaseTypeHandler<Double>(Double.class, 10) {
    @Override Double read(final Reader reader) throws Exception {
      return reader.readDouble();
    }
    @Override void write(final Double value, final Writer writer) throws Exception {
      writer.writeDouble(value);
    }
  };

  private static final TypeHandler BOOLEAN_ARRAY = new BaseTypeHandler<boolean[]>(boolean[].class, 11) {
    @Override boolean[] read(final Reader reader) throws Exception {
      final int length = reader.readVarInt();
      boolean[] value = new boolean[Math.min(length, 1024)];
      for (int i = 0; i < length; i++) {
        if (i >= value.length) {
          value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
        }
        value[i] = reader.readByte() != 0;
      }
      return value;
    }
    @Override void write(final boolean[] value, final Writer writer) throws Exception {
      writer.writeVarInt(value.length);
      for (final boolean v : value) {
        writer.writeByte(v ? (byte)1 : (byte)0);
      }
    }
  };

  static final BaseTypeHandler<byte[]> BYTE_ARRAY = new BaseTypeHandler<byte[]>(byte[].class, 12) {
    @Override byte[] read(final Reader reader) throws Exception {
      final int length = reader.readVarInt();
      byte[] value = new byte[Math.min(length, 1024)];
      for (int i = 0; i < length; i++) {
        if (i >= value.length) {
          value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
        }
        value[i] = reader.readByte();
      }
      return value;
    }
    @Override void write(final byte[] value, final Writer writer) throws Exception {
      writer.writeVarInt(value.length);
      writer.writeBytes(value);
    }
  };

  private static final TypeHandler SHORT_ARRAY = new BaseTypeHandler<short[]>(short[].class, 13) {
    @Override short[] read(final Reader reader) throws Exception {
      final int length = reader.readVarInt();
      short[] value = new short[Math.min(length, 512)];
      for (int i = 0; i < length; i++) {
        if (i >= value.length) {
          value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
        }
        //noinspection NumericCastThatLosesPrecision
        value[i] = (short)reader.readZigZagInt();
      }
      return value;
    }
    @Override void write(final short[] value, final Writer writer) throws Exception {
      writer.writeVarInt(value.length);
      for (final short v : value) {
        writer.writeZigZagInt(v);
      }
    }
  };

  private static final TypeHandler INTEGER_ARRAY = new BaseTypeHandler<int[]>(int[].class, 14) {
    @Override int[] read(final Reader reader) throws Exception {
      final int length = reader.readVarInt();
      int[] value = new int[Math.min(length, 256)];
      for (int i = 0; i < length; i++) {
        if (i >= value.length) {
          value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
        }
        value[i] = reader.readZigZagInt();
      }
      return value;
    }
    @Override void write(final int[] value, final Writer writer) throws Exception {
      writer.writeVarInt(value.length);
      for (final int v : value) {
        writer.writeZigZagInt(v);
      }
    }
  };

  private static final TypeHandler LONG_ARRAY = new BaseTypeHandler<long[]>(long[].class, 15) {
    @Override long[] read(final Reader reader) throws Exception {
      final int length = reader.readVarInt();
      long[] value = new long[Math.min(length, 128)];
      for (int i = 0; i < length; i++) {
        if (i >= value.length) {
          value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
        }
        value[i] = reader.readZigZagLong();
      }
      return value;
    }
    @Override void write(final long[] value, final Writer writer) throws Exception {
      writer.writeVarInt(value.length);
      for (final long v : value) {
        writer.writeZigZagLong(v);
      }
    }
  };

  private static final TypeHandler CHARACTER_ARRAY = new BaseTypeHandler<char[]>(char[].class, 16) {
    @Override char[] read(final Reader reader) throws Exception {
      final int length = reader.readVarInt();
      char[] value = new char[Math.min(length, 512)];
      for (int i = 0; i < length; i++) {
        if (i >= value.length) {
          value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
        }
        value[i] = reader.readChar();
      }
      return value;
    }
    @Override void write(final char[] value, final Writer writer) throws Exception {
      writer.writeVarInt(value.length);
      for (final char v : value) {
        writer.writeChar(v);
      }
    }
  };

  private static final TypeHandler FLOAT_ARRAY = new BaseTypeHandler<float[]>(float[].class, 17) {
    @Override float[] read(final Reader reader) throws Exception {
      final int length = reader.readVarInt();
      float[] value = new float[Math.min(length, 256)];
      for (int i = 0; i < length; i++) {
        if (i >= value.length) {
          value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
        }
        value[i] = reader.readFloat();
      }
      return value;
    }
    @Override void write(final float[] value, final Writer writer) throws Exception {
      writer.writeVarInt(value.length);
      for (final float v : value) {
        writer.writeFloat(v);
      }
    }
  };

  private static final TypeHandler DOUBLE_ARRAY = new BaseTypeHandler<double[]>(double[].class, 18) {
    @Override double[] read(final Reader reader) throws Exception {
      final int length = reader.readVarInt();
      double[] value = new double[Math.min(length, 128)];
      for (int i = 0; i < length; i++) {
        if (i >= value.length) {
          value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
        }
        value[i] = reader.readDouble();
      }
      return value;
    }
    @Override void write(final double[] value, final Writer writer) throws Exception {
      writer.writeVarInt(value.length);
      for (final double v : value) {
        writer.writeDouble(v);
      }
    }
  };

  static final TypeHandler STRING = new BaseTypeHandler<String>(String.class, 19) {
    @Override String read(final Reader reader) throws Exception {
      return Utf8.string(BYTE_ARRAY.read(reader));
    }
    @Override void write(final String value, final Writer writer) throws Exception {
      BYTE_ARRAY.write(Utf8.bytes(value), writer);
    }
  };

  static final List<TypeHandler> ALL = Collections.unmodifiableList(Arrays.asList(
    NULL,
    REFERENCE,
    LIST,
    BOOLEAN,
    BYTE,
    SHORT,
    INTEGER,
    LONG,
    CHARACTER,
    FLOAT,
    DOUBLE,
    BOOLEAN_ARRAY,
    BYTE_ARRAY,
    SHORT_ARRAY,
    INTEGER_ARRAY,
    LONG_ARRAY,
    CHARACTER_ARRAY,
    FLOAT_ARRAY,
    DOUBLE_ARRAY,
    STRING
  ));

  static {
    // sanity checks
    int id = 0;
    for (final TypeHandler typeHandler : ALL) {
      if (typeHandler.id != id++) {
        throw new RuntimeException("typeHandler " + typeHandler.id + " has wrong number");
      }
    }
  }

  static final int SIZE = ALL.size();

}
