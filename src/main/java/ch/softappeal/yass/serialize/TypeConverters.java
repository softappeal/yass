package ch.softappeal.yass.serialize;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class TypeConverters {

  private TypeConverters() {
    // disable
  }

  public static <E extends Enum<E>> TypeConverter<E, Integer> enumToInteger(final Class<E> enumeration) {
    final E[] constants = enumeration.getEnumConstants();
    return new TypeConverter<E, Integer>(enumeration, Integer.class) {
      @Override public Integer to(final E value) {
        return value.ordinal();
      }
      @Override public E from(final Integer value) {
        return constants[value];
      }
    };
  }

  public static final TypeConverter<BigInteger, String> BIGINTEGER_TO_STRING = new TypeConverter<BigInteger, String>(BigInteger.class, String.class) {
    @Override public String to(final BigInteger value) {
      return value.toString();
    }
    @Override public BigInteger from(final String value) {
      return new BigInteger(value);
    }
  };

  public static final TypeConverter<BigDecimal, String> BIGDECIMAL_TO_STRING = new TypeConverter<BigDecimal, String>(BigDecimal.class, String.class) {
    @Override public String to(final BigDecimal value) {
      return value.toString();
    }
    @Override public BigDecimal from(final String value) {
      return new BigDecimal(value);
    }
  };

}
