package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Writer;

public abstract class BaseTypeHandler<V> extends TypeHandler {

  BaseTypeHandler(final Class<?> type, final int id) {
    super(type, id);
  }

  abstract V read(Reader reader) throws Exception;

  @Override final Object readNoId(final Input input) throws Exception {
    return read(input.reader);
  }

  abstract void write(V value, Writer writer) throws Exception;

  @SuppressWarnings("unchecked")
  @Override final void writeNoId(final Object value, final Output output) throws Exception {
    write((V)value, output.writer);
  }

}
