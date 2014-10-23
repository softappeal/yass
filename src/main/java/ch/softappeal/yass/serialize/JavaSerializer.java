package ch.softappeal.yass.serialize;

import ch.softappeal.yass.util.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A {@link Serializer} using {@link Serializable}.
 */
public final class JavaSerializer implements Serializer {

  private JavaSerializer() {
    // empty
  }

  @Override @Nullable public Object read(final Reader reader) throws Exception {
    try (ObjectInputStream in = new ObjectInputStream(reader.stream())) {
      return in.readObject();
    }
  }

  @Override public void write(@Nullable final Object value, final Writer writer) throws IOException {
    try (ObjectOutputStream out = new ObjectOutputStream(writer.stream())) {
      out.writeObject(value);
    }
  }

  public static final Serializer INSTANCE = new JavaSerializer();

}
