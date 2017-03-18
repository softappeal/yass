package ch.softappeal.yass.util;

import java.io.InputStream;

/**
 * Needed for backward compatibility, use {@link InputStreamSupplier} instead.
 */
@FunctionalInterface public interface Resource {

    @Deprecated
    InputStream create();

    @Deprecated
    static Resource convert(final InputStreamSupplier supplier) {
        return supplier::get;
    }

}
