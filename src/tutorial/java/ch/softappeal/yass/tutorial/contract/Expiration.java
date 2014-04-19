package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandler;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;

/**
 * Shows how to use a new base type.
 */
public final class Expiration {

  public final int year;
  public final int month;
  public final int day;

  public Expiration(final int year, final int month, final int day) {
    this.year = year;
    this.month = month;
    this.day = day;
  }

  public static final BaseTypeHandler<?> TYPE_HANDLER = new BaseTypeHandler<Expiration>(Expiration.class) {
    @Override public Expiration read(final Reader reader) throws Exception {
      return new Expiration(
        BaseTypeHandlers.INTEGER.read(reader),
        BaseTypeHandlers.INTEGER.read(reader),
        BaseTypeHandlers.INTEGER.read(reader)
      );
    }
    @Override public void write(final Expiration value, final Writer writer) throws Exception {
      BaseTypeHandlers.INTEGER.write(value.year, writer);
      BaseTypeHandlers.INTEGER.write(value.month, writer);
      BaseTypeHandlers.INTEGER.write(value.day, writer);
    }
  };

}
