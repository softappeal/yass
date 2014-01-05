package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandler;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.TypeDesc;
import ch.softappeal.yass.util.Check;

/**
 * Shows how to use a base {@link TypeDesc}.
 */
public final class DateTime {

  public final String value;

  public DateTime(final String value) {
    this.value = Check.notNull(value);
  }

  public static final TypeDesc TYPE_DESC = new TypeDesc(100, new BaseTypeHandler<DateTime>(DateTime.class) {
    @Override public DateTime read(final Reader reader) throws Exception {
      return new DateTime(BaseTypeHandlers.STRING.read(reader));
    }
    @Override public void write(final DateTime value, final Writer writer) throws Exception {
      BaseTypeHandlers.STRING.write(value.value, writer);
    }
  });

}
