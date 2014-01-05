package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Writer;

public abstract class BaseTypeHandler<V> extends TypeHandler {

  protected BaseTypeHandler(final Class<V> type) {
    super(type);
  }

  public abstract V read(Reader reader) throws Exception;

  @Override final Object read(final Input input) throws Exception {
    return read(input.reader);
  }

  public abstract void write(V value, Writer writer) throws Exception;

  @SuppressWarnings("unchecked")
  @Override final void write(final Object value, final Output output) throws Exception {
    write((V)value, output.writer);
  }

}
