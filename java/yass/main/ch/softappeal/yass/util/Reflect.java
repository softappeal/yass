package ch.softappeal.yass.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Reflect {

    private Reflect() {
        // disable
    }

    public static List<Field> ownFields(final Class<?> type) {
        final List<Field> fields = new ArrayList<>(16);
        for (final var field : type.getDeclaredFields()) {
            final var modifiers = field.getModifiers();
            if (!(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
                fields.add(field);
                if (!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers)) {
                    field.setAccessible(true);
                }
            }
        }
        fields.sort(Comparator.comparing(Field::getName));
        return fields;
    }

    public static List<Field> allFields(final Class<?> type) {
        final List<Field> fields = new ArrayList<>(16);
        for (var t = Objects.requireNonNull(type); (t != null) && (t != Throwable.class); t = t.getSuperclass()) {
            fields.addAll(ownFields(t));
        }
        return fields;
    }

    public static final Function<Class<?>, Supplier<Object>> ALLOCATE = type -> () -> {
        try {
            return Unsafe.INSTANCE.allocateInstance(type);
        } catch (final InstantiationException e) {
            throw new RuntimeException(e);
        }
    };

}
