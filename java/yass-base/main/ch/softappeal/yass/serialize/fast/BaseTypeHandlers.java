package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Utf8;
import ch.softappeal.yass.serialize.Writer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

public final class BaseTypeHandlers {

    private BaseTypeHandlers() {
        // disable
    }

    public static final BaseTypeHandler<Boolean> BOOLEAN = new BaseTypeHandler<>(Boolean.class) {
        @Override public Boolean read(final Reader reader) throws Exception {
            return reader.readByte() != 0;
        }
        @Override public void write(final Boolean value, final Writer writer) throws Exception {
            writer.writeByte(value ? (byte)1 : (byte)0);
        }
    };

    public static final BaseTypeHandler<Byte> BYTE = new BaseTypeHandler<>(Byte.class) {
        @Override public Byte read(final Reader reader) throws Exception {
            return reader.readByte();
        }
        @Override public void write(final Byte value, final Writer writer) throws Exception {
            writer.writeByte(value);
        }
    };

    public static final BaseTypeHandler<Short> SHORT = new BaseTypeHandler<>(Short.class) {
        @Override public Short read(final Reader reader) throws Exception {
            return (short)reader.readZigZagInt();
        }
        @Override public void write(final Short value, final Writer writer) throws Exception {
            writer.writeZigZagInt(value);
        }
    };

    public static final BaseTypeHandler<Integer> INTEGER = new BaseTypeHandler<>(Integer.class) {
        @Override public Integer read(final Reader reader) throws Exception {
            return reader.readZigZagInt();
        }
        @Override public void write(final Integer value, final Writer writer) throws Exception {
            writer.writeZigZagInt(value);
        }
    };

    public static final BaseTypeHandler<Long> LONG = new BaseTypeHandler<>(Long.class) {
        @Override public Long read(final Reader reader) throws Exception {
            return reader.readZigZagLong();
        }
        @Override public void write(final Long value, final Writer writer) throws Exception {
            writer.writeZigZagLong(value);
        }
    };

    public static final BaseTypeHandler<Character> CHARACTER = new BaseTypeHandler<>(Character.class) {
        @Override public Character read(final Reader reader) throws Exception {
            return reader.readChar();
        }
        @Override public void write(final Character value, final Writer writer) throws Exception {
            writer.writeChar(value);
        }
    };

    public static final BaseTypeHandler<Float> FLOAT = new BaseTypeHandler<>(Float.class) {
        @Override public Float read(final Reader reader) throws Exception {
            return reader.readFloat();
        }
        @Override public void write(final Float value, final Writer writer) throws Exception {
            writer.writeFloat(value);
        }
    };

    public static final BaseTypeHandler<Double> DOUBLE = new BaseTypeHandler<>(Double.class) {
        @Override public Double read(final Reader reader) throws Exception {
            return reader.readDouble();
        }
        @Override public void write(final Double value, final Writer writer) throws Exception {
            writer.writeDouble(value);
        }
    };

    public static final BaseTypeHandler<boolean[]> BOOLEAN_ARRAY = new BaseTypeHandler<>(boolean[].class) {
        @Override public boolean[] read(final Reader reader) throws Exception {
            final int length = reader.readVarInt();
            boolean[] value = new boolean[Math.min(length, 128)];
            for (int i = 0; i < length; i++) {
                if (i >= value.length) {
                    value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
                }
                value[i] = reader.readByte() != 0;
            }
            return value;
        }
        @Override public void write(final boolean[] value, final Writer writer) throws Exception {
            writer.writeVarInt(value.length);
            for (final boolean v : value) {
                writer.writeByte(v ? (byte)1 : (byte)0);
            }
        }
    };

    public static final BaseTypeHandler<byte[]> BYTE_ARRAY = new BaseTypeHandler<>(byte[].class) {
        @Override public byte[] read(final Reader reader) throws Exception {
            final int length = reader.readVarInt();
            byte[] value = new byte[Math.min(length, 128)];
            int i = 0;
            while (i < length) {
                if (i >= value.length) {
                    value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
                }
                final int l = value.length - i;
                reader.readBytes(value, i, l);
                i += l;
            }
            return value;
        }
        @Override public void write(final byte[] value, final Writer writer) throws Exception {
            writer.writeVarInt(value.length);
            writer.writeBytes(value);
        }
    };

    public static final BaseTypeHandler<short[]> SHORT_ARRAY = new BaseTypeHandler<>(short[].class) {
        @Override public short[] read(final Reader reader) throws Exception {
            final int length = reader.readVarInt();
            short[] value = new short[Math.min(length, 64)];
            for (int i = 0; i < length; i++) {
                if (i >= value.length) {
                    value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
                }
                value[i] = (short)reader.readZigZagInt();
            }
            return value;
        }
        @Override public void write(final short[] value, final Writer writer) throws Exception {
            writer.writeVarInt(value.length);
            for (final short v : value) {
                writer.writeZigZagInt(v);
            }
        }
    };

    public static final BaseTypeHandler<int[]> INTEGER_ARRAY = new BaseTypeHandler<>(int[].class) {
        @Override public int[] read(final Reader reader) throws Exception {
            final int length = reader.readVarInt();
            int[] value = new int[Math.min(length, 32)];
            for (int i = 0; i < length; i++) {
                if (i >= value.length) {
                    value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
                }
                value[i] = reader.readZigZagInt();
            }
            return value;
        }
        @Override public void write(final int[] value, final Writer writer) throws Exception {
            writer.writeVarInt(value.length);
            for (final int v : value) {
                writer.writeZigZagInt(v);
            }
        }
    };

    public static final BaseTypeHandler<long[]> LONG_ARRAY = new BaseTypeHandler<>(long[].class) {
        @Override public long[] read(final Reader reader) throws Exception {
            final int length = reader.readVarInt();
            long[] value = new long[Math.min(length, 16)];
            for (int i = 0; i < length; i++) {
                if (i >= value.length) {
                    value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
                }
                value[i] = reader.readZigZagLong();
            }
            return value;
        }
        @Override public void write(final long[] value, final Writer writer) throws Exception {
            writer.writeVarInt(value.length);
            for (final long v : value) {
                writer.writeZigZagLong(v);
            }
        }
    };

    public static final BaseTypeHandler<char[]> CHARACTER_ARRAY = new BaseTypeHandler<>(char[].class) {
        @Override public char[] read(final Reader reader) throws Exception {
            final int length = reader.readVarInt();
            char[] value = new char[Math.min(length, 64)];
            for (int i = 0; i < length; i++) {
                if (i >= value.length) {
                    value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
                }
                value[i] = reader.readChar();
            }
            return value;
        }
        @Override public void write(final char[] value, final Writer writer) throws Exception {
            writer.writeVarInt(value.length);
            for (final char v : value) {
                writer.writeChar(v);
            }
        }
    };

    public static final BaseTypeHandler<float[]> FLOAT_ARRAY = new BaseTypeHandler<>(float[].class) {
        @Override public float[] read(final Reader reader) throws Exception {
            final int length = reader.readVarInt();
            float[] value = new float[Math.min(length, 32)];
            for (int i = 0; i < length; i++) {
                if (i >= value.length) {
                    value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
                }
                value[i] = reader.readFloat();
            }
            return value;
        }
        @Override public void write(final float[] value, final Writer writer) throws Exception {
            writer.writeVarInt(value.length);
            for (final float v : value) {
                writer.writeFloat(v);
            }
        }
    };

    public static final BaseTypeHandler<double[]> DOUBLE_ARRAY = new BaseTypeHandler<>(double[].class) {
        @Override public double[] read(final Reader reader) throws Exception {
            final int length = reader.readVarInt();
            double[] value = new double[Math.min(length, 16)];
            for (int i = 0; i < length; i++) {
                if (i >= value.length) {
                    value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
                }
                value[i] = reader.readDouble();
            }
            return value;
        }
        @Override public void write(final double[] value, final Writer writer) throws Exception {
            writer.writeVarInt(value.length);
            for (final double v : value) {
                writer.writeDouble(v);
            }
        }
    };

    public static final BaseTypeHandler<String> STRING = new BaseTypeHandler<>(String.class) {
        @Override public String read(final Reader reader) throws Exception {
            return Utf8.string(BYTE_ARRAY.read(reader));
        }
        @Override public void write(final String value, final Writer writer) throws Exception {
            BYTE_ARRAY.write(Utf8.bytes(value), writer);
        }
    };

    public static final BaseTypeHandler<Date> DATE = new BaseTypeHandler<>(Date.class) {
        @Override public Date read(final Reader reader) throws Exception {
            return new Date(LONG.read(reader));
        }
        @Override public void write(final Date value, final Writer writer) throws Exception {
            LONG.write(value.getTime(), writer);
        }
    };

    public static final BaseTypeHandler<Instant> INSTANT = new BaseTypeHandler<>(Instant.class) {
        @Override public Instant read(final Reader reader) throws Exception {
            return Instant.ofEpochSecond(LONG.read(reader), reader.readVarInt());
        }
        @Override public void write(final Instant value, final Writer writer) throws Exception {
            LONG.write(value.getEpochSecond(), writer);
            writer.writeVarInt(value.getNano());
        }
    };

    public static final BaseTypeHandler<BigInteger> BIGINTEGER = new BaseTypeHandler<>(BigInteger.class) {
        @Override public BigInteger read(final Reader reader) throws Exception {
            return new BigInteger(BYTE_ARRAY.read(reader));
        }
        @Override public void write(final BigInteger value, final Writer writer) throws Exception {
            BYTE_ARRAY.write(value.toByteArray(), writer);
        }
    };

    public static final BaseTypeHandler<BigDecimal> BIGDECIMAL = new BaseTypeHandler<>(BigDecimal.class) {
        @Override public BigDecimal read(final Reader reader) throws Exception {
            return new BigDecimal(BIGINTEGER.read(reader), INTEGER.read(reader));
        }
        @Override public void write(final BigDecimal value, final Writer writer) throws Exception {
            BIGINTEGER.write(value.unscaledValue(), writer);
            INTEGER.write(value.scale(), writer);
        }
    };

}
