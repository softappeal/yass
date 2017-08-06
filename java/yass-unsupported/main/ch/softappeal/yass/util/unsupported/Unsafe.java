package ch.softappeal.yass.util.unsupported;

import ch.softappeal.yass.util.Exceptions;

import java.lang.reflect.Field;

final class Unsafe {

    private Unsafe() {
        // disable
    }

    static final sun.misc.Unsafe INSTANCE;

    static {
        try {
            final Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            INSTANCE = (sun.misc.Unsafe)field.get(null);
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

}
