package ch.softappeal.yass.serialize.contract;

import ch.softappeal.yass.serialize.convert.LongTypeConverter;

import java.io.Serializable;

public final class LongClass implements Serializable {

  private static final long serialVersionUID = 1L;

  public LongClass(final long value) {
    this.value = value;
  }

  public final long value;

  public static final LongTypeConverter TYPE_CONVERTER = new LongTypeConverter(LongClass.class) {
    @Override public Object fromLong(final long value) {
      return new LongClass(value);
    }
    @Override public long toLong(final Object value) {
      return ((LongClass)value).value;
    }
  };

}
