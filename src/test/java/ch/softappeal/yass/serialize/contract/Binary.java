package ch.softappeal.yass.serialize.contract;

import ch.softappeal.yass.serialize.convert.BinaryTypeConverter;

import java.io.Serializable;

public final class Binary implements Serializable {

  private static final long serialVersionUID = 1L;

  public Binary(final byte[] value) {
    this.value = value.clone();
  }

  private final byte[] value;

  public byte[] value() {
    return value.clone();
  }

  @Override public String toString() {
    final StringBuilder s = new StringBuilder(256);
    s.append('[');
    for (final byte b : value) {
      s.append(' ').append(b);
    }
    s.append(" ]");
    return s.toString();
  }

  public static final BinaryTypeConverter TYPE_CONVERTER = new BinaryTypeConverter(Binary.class) {
    @Override public Object fromBinary(final byte[] value) {
      return new Binary(value);
    }
    @Override public byte[] toBinary(final Object value) {
      return ((Binary)value).value();
    }
  };

}
