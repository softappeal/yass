package ch.softappeal.yass.serialize.convert;

public abstract class IntegerTypeConverter extends TypeConverter {

  protected IntegerTypeConverter(final Class<?> type) {
    super(type);
  }

  public abstract Object fromInteger(int value) throws Exception;

  public abstract int toInteger(Object value) throws Exception;

  @Override public final void accept(final Visitor visitor) {
    visitor.visit(this);
  }

  /**
   * Uses {@link Enum#ordinal()}.
   */
  public static <T extends Enum<T>> IntegerTypeConverter forEnum(final Class<T> enumeration) {
    final Enum<T>[] constants = enumeration.getEnumConstants();
    return new IntegerTypeConverter(enumeration) {
      @Override public Object fromInteger(final int value) {
        return constants[value];
      }
      @SuppressWarnings("unchecked")
      @Override public int toInteger(final Object value) {
        return ((Enum<T>)value).ordinal();
      }
    };
  }

}
