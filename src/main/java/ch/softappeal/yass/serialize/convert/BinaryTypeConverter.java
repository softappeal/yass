package ch.softappeal.yass.serialize.convert;

public abstract class BinaryTypeConverter extends TypeConverter {

  protected BinaryTypeConverter(final Class<?> type) {
    super(type);
  }

  public abstract Object fromBinary(byte[] value) throws Exception;

  public abstract byte[] toBinary(Object value) throws Exception;

  @Override public final void accept(final Visitor visitor) {
    visitor.visit(this);
  }

}
