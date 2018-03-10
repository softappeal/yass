package ch.softappeal.yass.util;

import java.lang.reflect.AnnotatedElement;

public final class Tags {

    private Tags() {
        // disable
    }

    /**
     * @throws IllegalArgumentException if missing {@link Tag}
     */
    public static int getTag(final AnnotatedElement element) throws IllegalArgumentException {
        final var annotation = element.getAnnotation(Tag.class);
        if (annotation == null) {
            throw new IllegalArgumentException("missing tag for '" + element + '\'');
        }
        return annotation.value();
    }

}
