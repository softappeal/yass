package ch.softappeal.yass.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Instantiators {

    private Instantiators() {
        // empty
    }

    /**
     * Class to be instantiated must have a no-argument constructor.
     */
    public static final Function<Class<?>, Supplier<Object>> NOARG = type -> {
        final Constructor<?> constructor;
        try {
            constructor = type.getDeclaredConstructor();
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        if (!Modifier.isPublic(constructor.getModifiers())) {
            constructor.setAccessible(true);
        }
        return () -> {
            try {
                return constructor.newInstance();
            } catch (final Exception e) {
                throw Exceptions.wrap(e);
            }
        };
    };

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
