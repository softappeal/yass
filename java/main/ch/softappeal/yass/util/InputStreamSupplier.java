package ch.softappeal.yass.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.function.Supplier;

@FunctionalInterface public interface InputStreamSupplier extends Supplier<InputStream> {

    static InputStreamSupplier create(final File file) {
        Check.notNull(file);
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
        Check.notNull(classLoader);
        Check.notNull(name);
        return () -> {
            final @Nullable InputStream in = classLoader.getResourceAsStream(name);
            if (in == null) {
                throw new RuntimeException("resource '" + name + "' not found");
            }
            return in;
        };
    }

}
