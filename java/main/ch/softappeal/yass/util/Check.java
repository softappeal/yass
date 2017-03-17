package ch.softappeal.yass.util;

/**
 * Check utilities.
 */
public final class Check {

    /**
     * @param <T> the value type
     * @return value if (value != null)
     * @throws NullPointerException if (value == null)
     */
    public static <T> T notNull(final @Nullable T value) throws NullPointerException {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

}
