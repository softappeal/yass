package ch.softappeal.yass.util.unsupported;

import java.util.function.Function;
import java.util.function.Supplier;

public final class UnsupportedInstantiators {

    private UnsupportedInstantiators() {
        // disable
    }

    /**
     * Works with any class.
     */
    public static final Function<Class<?>, Supplier<Object>> UNSAFE = type -> () -> {
        try {
            return Unsafe.INSTANCE.allocateInstance(type);
        } catch (final InstantiationException e) {
            throw new RuntimeException(e);
        }
    };

}
