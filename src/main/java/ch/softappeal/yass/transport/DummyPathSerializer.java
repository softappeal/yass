package ch.softappeal.yass.transport;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;

public final class DummyPathSerializer implements Serializer {

  public static final Integer PATH = 0;

  private DummyPathSerializer() {
    // disable
  }

  @Override public Object read(final Reader reader) {
    return PATH;
  }

  @Override public void write(final Object value, final Writer writer) {
    // empty
  }

  public static final Serializer INSTANCE = new DummyPathSerializer();

}
