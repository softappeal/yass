package ch.softappeal.yass.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Reflect {

    private Reflect() {
        // disable
    }

    public static final Unsafe UNSAFE;

    static {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe)field.get(null);
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    public static List<Field> ownFields(final Class<?> type) {
        final List<Field> fields = new ArrayList<>(16);
        for (final Field field : type.getDeclaredFields()) {
            final int modifiers = field.getModifiers();
            if (!(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
                fields.add(field);
            }
        }
        Collections.sort(fields, (field1, field2) -> field1.getName().compareTo(field2.getName()));
        return fields;
    }

    public static List<Field> allFields(Class<?> type) {
        Check.notNull(type);
        final List<Field> fields = new ArrayList<>(16);
        while ((type != null) && (type != Throwable.class)) {
            fields.addAll(ownFields(type));
            type = type.getSuperclass();
        }
        return fields;
    }

}
