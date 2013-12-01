package ch.softappeal.yass.serialize.convert;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class StringTypeConverter extends TypeConverter {

  protected StringTypeConverter(final Class<?> type) {
    super(type);
  }

  public abstract Object fromString(String value) throws Exception;

  public abstract String toString(Object value) throws Exception;

  @Override public final void accept(final Visitor visitor) {
    visitor.visit(this);
  }

  public static final StringTypeConverter BIG_DECIMAL = new StringTypeConverter(BigDecimal.class) {
    @Override public Object fromString(final String value) {
      return new BigDecimal(value);
    }
    @Override public String toString(final Object value) {
      return value.toString();
    }
  };

  public static final StringTypeConverter BIG_INTEGER = new StringTypeConverter(BigInteger.class) {
    @Override public Object fromString(final String value) {
      return new BigInteger(value);
    }
    @Override public String toString(final Object value) {
      return value.toString();
    }
  };

  /**
   * Uses {@link Enum#name()}.
   */
  public static <T extends Enum<T>> StringTypeConverter forEnum(final Class<T> enumeration) {
    return new StringTypeConverter(enumeration) {
      @Override public Object fromString(final String value) {
        return Enum.valueOf(enumeration, value);
      }
      @SuppressWarnings("unchecked")
      @Override public String toString(final Object value) {
        return ((Enum<T>)value).name();
      }
    };
  }

}
