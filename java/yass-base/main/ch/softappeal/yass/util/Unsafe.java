package ch.softappeal.yass.util;

final class Unsafe {

    private Unsafe() {
        // disable
    }

    static final sun.misc.Unsafe INSTANCE;

    static {
        try {
            final var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            INSTANCE = (sun.misc.Unsafe)field.get(null);
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
