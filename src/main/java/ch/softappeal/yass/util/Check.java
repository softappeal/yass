package ch.softappeal.yass.util;

import java.lang.reflect.AnnotatedElement;

/**
 * Check utilities.
 */
public final class Check {

    private Check() {
        // disable
    }

    /**
     * @param <T> the value type
     * @return value if (value != null)
     * @throws NullPointerException if (value == null)
     */
    public static <T> T notNull(@Nullable final T value) throws NullPointerException {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

    /**
     * @throws IllegalArgumentException if missing {@link Tag}
     */
    public static int hasTag(final AnnotatedElement element) throws IllegalArgumentException {
        final Tag annotation = element.getAnnotation(Tag.class);
        if (annotation == null) {
            throw new IllegalArgumentException("missing tag for '" + element + '\'');
        }
        return annotation.value();
    }

}
