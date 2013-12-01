package ch.softappeal.yass.serialize.convert;

public abstract class LongTypeConverter extends TypeConverter {

  protected LongTypeConverter(final Class<?> type) {
    super(type);
  }

  public abstract Object fromLong(long value) throws Exception;

  public abstract long toLong(Object value) throws Exception;

  @Override public final void accept(final Visitor visitor) {
    visitor.visit(this);
  }

}
