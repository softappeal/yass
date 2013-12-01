package ch.softappeal.yass.serialize.convert;

import ch.softappeal.yass.util.Check;

import java.io.Serializable;

/**
 * Base class for {@link String} based value classes.
 */
public abstract class StringValue implements Serializable {

  private static final long serialVersionUID = 1L;

  public final String value;

  protected StringValue(final String value) {
    this.value = Check.notNull(value);
  }

  @Override public String toString() {
    return value;
  }

  public abstract static class TypeConverter extends StringTypeConverter {
    protected TypeConverter(final Class<? extends StringValue> type) {
      super(type);
    }
    @Override public final String toString(final Object value) {
      return ((StringValue)value).value;
    }
  }

}
