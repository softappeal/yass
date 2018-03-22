package ch.softappeal.yass.util;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

public final class Tags {

    private Tags() {
        // disable
    }

    /**
     * @throws IllegalArgumentException if missing {@link Tag}
     */
    public static int getTag(final AnnotatedElement element) throws IllegalArgumentException {
        return Optional.ofNullable(element.getAnnotation(Tag.class))
            .orElseThrow(() -> new IllegalArgumentException("missing tag for '" + element + '\''))
            .value();
    }

}
