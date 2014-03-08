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
    final ObjectInputStream in = new ObjectInputStream(reader.stream());
    try {
      return in.readObject();
    } finally {
      in.close();
    }
  }

  @Override public void write(@Nullable final Object value, final Writer writer) throws IOException {
    final ObjectOutputStream out = new ObjectOutputStream(writer.stream());
    try {
      out.writeObject(value);
    } finally {
      out.close();
    }
  }

  public static final JavaSerializer INSTANCE = new JavaSerializer();

}
