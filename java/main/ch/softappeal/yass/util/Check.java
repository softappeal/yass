package ch.softappeal.yass.util;

import java.util.Objects;

/**
 * Needed for backward compatibility, use {@link Objects#requireNonNull(Object)} instead.
 */
public final class Check {

    @Deprecated
    public static <T> T notNull(final @Nullable T value) throws NullPointerException {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

}
