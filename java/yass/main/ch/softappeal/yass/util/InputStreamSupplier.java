package ch.softappeal.yass.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Supplier;

@FunctionalInterface public interface InputStreamSupplier extends Supplier<InputStream> {

    static InputStreamSupplier create(final File file) {
        Objects.requireNonNull(file);
        return () -> {
            try {
                return new FileInputStream(file);
            } catch (final FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        };
    }

    static InputStreamSupplier create(final String file) {
        return create(new File(file));
    }

    static InputStreamSupplier create(final ClassLoader classLoader, final String name) {
        Objects.requireNonNull(classLoader);
        Objects.requireNonNull(name);
        return () -> {
            final var in = classLoader.getResourceAsStream(name);
            if (in == null) {
                throw new RuntimeException("resource '" + name + "' not found");
            }
            return in;
        };
    }

}
