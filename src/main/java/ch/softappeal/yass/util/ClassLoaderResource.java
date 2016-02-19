package ch.softappeal.yass.util;

import java.io.InputStream;

public final class ClassLoaderResource implements Resource {

    private final ClassLoader classLoader;
    private final String name;

    public ClassLoaderResource(final ClassLoader classLoader, final String name) {
        this.classLoader = Check.notNull(classLoader);
        this.name = Check.notNull(name);
    }

    @Override public InputStream create() {
        final @Nullable InputStream in = classLoader.getResourceAsStream(name);
        if (in == null) {
            throw new RuntimeException("resource '" + name + "' not found");
        }
        return in;
    }

}
