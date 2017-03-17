package ch.softappeal.yass.util;

import java.util.Objects;

/**
 * @deprecated needed for backward compatibility
 */
@Deprecated
public final class Check {

    /**
     * @deprecated use {@link Objects#requireNonNull(Object)}
     */
    @Deprecated
    public static <T> T notNull(final @Nullable T value) throws NullPointerException {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

}
