package ch.softappeal.yass.serialize;

import ch.softappeal.yass.Nullable;

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

    @Override public @Nullable Object read(final Reader reader) throws Exception {
        try (var in = new ObjectInputStream(reader.stream())) {
            return in.readObject();
        }
    }

    @Override public void write(final @Nullable Object value, final Writer writer) throws IOException {
        try (var out = new ObjectOutputStream(writer.stream())) {
            out.writeObject(value);
        }
    }

    public static final Serializer INSTANCE = new JavaSerializer();

}
