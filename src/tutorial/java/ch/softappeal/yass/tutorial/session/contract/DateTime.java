package ch.softappeal.yass.tutorial.session.contract;

import ch.softappeal.yass.serialize.TypeConverter;
import ch.softappeal.yass.util.Check;

/**
 * Shows how to use {@link TypeConverter}.
 */
public final class DateTime {


  public final String value;

  public DateTime(final String value) {
    this.value = Check.notNull(value);
  }

  @Override public String toString() {
    return value;
  }


  public static final TypeConverter<DateTime, String> TO_STRING = new TypeConverter<DateTime, String>(DateTime.class, String.class) {
    @Override public String to(final DateTime value) {
      return value.value;
    }
    @Override public DateTime from(final String value) {
      return new DateTime(value);
    }
  };


}
