package ch.softappeal.yass.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This implementation is not very efficient! It uses uncached reflection.
 */
public final class Dumper {

    private static final Set<Class<?>> PRIMITIVE_WRAPPER_CLASSES = new HashSet<>(Arrays.asList(
        Boolean.class,
        Character.class,
        Byte.class,
        Short.class,
        Integer.class,
        Long.class,
        Float.class,
        Double.class
    ));

    private final boolean compact;
    private final boolean cycles;
    private final Set<Class<?>> concreteValueClasses;

    /**
     * @param compact one-liner or multiple lines
     * @param cycles graph can have cycles; these are marked with #
     * @param concreteValueClasses should implement {@link Object#toString()}; only allowed if (cycles)
     */
    public Dumper(final boolean compact, final boolean cycles, final Class<?>... concreteValueClasses) {
        this.compact = compact;
        this.cycles = cycles;
        if (!cycles && (concreteValueClasses.length != 0)) {
            throw new IllegalArgumentException("concreteValueClasses only allowed if (cycles)");
        }
        this.concreteValueClasses = new HashSet<>(Arrays.asList(concreteValueClasses));
        this.concreteValueClasses.addAll(PRIMITIVE_WRAPPER_CLASSES);
    }

    /**
     * Calls {@code this(false, true, concreteValueClasses)}.
     */
    public Dumper(final Class<?>... concreteValueClasses) {
        this(false, true, concreteValueClasses);
    }

    public StringBuilder append(final StringBuilder s, @Nullable final Object value) {
        try {
            new Dump(s).dump(value);
            return s;
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    public String toString(@Nullable final Object value) {
        return append(new StringBuilder(256), value).toString();
    }

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final class Dump {

        private final StringBuilder out;

        Dump(final StringBuilder out) {
            this.out = Check.notNull(out);
        }

        private int tabs = 0;

        private void appendTabs() {
            for (int t = tabs; t > 0; t--) {
                out.append("    ");
            }
        }

        private void appendLine() {
            out.append(LINE_SEPARATOR);
        }

        private void inc(final String s) {
            out.append(s);
            appendLine();
            tabs++;
        }

        private void dec(final String s) {
            tabs--;
            appendTabs();
            out.append(s);
        }

        private void dumpArray(final Object array) throws Exception {
            final int length = Array.getLength(array);
            if (compact) {
                out.append("[ ");
                for (int i = 0; i < length; i++) {
                    dump(Array.get(array, i));
                    out.append(' ');
                }
                out.append(']');
            } else {
                inc("[");
                for (int i = 0; i < length; i++) {
                    appendTabs();
                    dump(Array.get(array, i));
                    appendLine();
                }
                dec("]");
            }
        }

        private void dumpCollection(final Collection<?> collection) throws Exception {
            if (compact) {
                out.append("[ ");
                for (final Object e : collection) {
                    dump(e);
                    out.append(' ');
                }
                out.append(']');
            } else {
                inc("[");
                for (final Object e : collection) {
                    appendTabs();
                    dump(e);
                    appendLine();
                }
                dec("]");
            }
        }

        private void dumpMap(final Map<?, ?> map) throws Exception {
            if (compact) {
                out.append("{ ");
                for (final Map.Entry<?, ?> entry : map.entrySet()) {
                    dump(entry.getKey());
                    out.append("->");
                    dump(entry.getValue());
                    out.append(' ');
                }
                out.append('}');
            } else {
                inc("{");
                for (final Map.Entry<?, ?> entry : map.entrySet()) {
                    appendTabs();
                    dump(entry.getKey());
                    out.append(" -> ");
                    dump(entry.getValue());
                    appendLine();
                }
                dec("}");
            }
        }

        private void dumpClassFields(Class<?> type, final Object object) throws Exception {
            while ((type != Object.class) && (type != Throwable.class)) {
                for (final Field field : type.getDeclaredFields()) {
                    final int modifiers = field.getModifiers();
                    if (!(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
                        field.setAccessible(true);
                        @Nullable final Object value = field.get(object);
                        if (value != null) {
                            if (compact) {
                                out.append(field.getName()).append('=');
                                dump(value);
                                out.append(' ');
                            } else {
                                appendTabs();
                                out.append(field.getName()).append(" = ");
                                dump(value);
                                appendLine();
                            }
                        }
                    }
                }
                type = type.getSuperclass();
            }
        }

        private final Map<Object, Integer> alreadyDumpedObjects = cycles ? new IdentityHashMap<>(16) : null;

        private void dumpClass(final Class<?> type, final Object object) throws Exception {
            final int index;
            if (cycles) {
                final Integer reference = alreadyDumpedObjects.get(object);
                if (reference != null) {
                    out.append('#').append(reference);
                    return;
                }
                index = alreadyDumpedObjects.size();
                alreadyDumpedObjects.put(object, index);
            } else {
                index = 0;
            }
            final Class<?> toStringClass = type.getMethod("toString").getDeclaringClass();
            if ((toStringClass == Object.class) || (toStringClass == Throwable.class)) {
                if (compact) {
                    out.append(type.getSimpleName()).append("( ");
                    dumpClassFields(type, object);
                    out.append(')');
                } else {
                    inc(type.getSimpleName() + '(');
                    dumpClassFields(type, object);
                    dec(")");
                }
            } else {
                out.append(object);
            }
            if (cycles) {
                out.append('#').append(index);
            }
        }

        void dump(@Nullable final Object value) throws Exception {
            if (value == null) {
                out.append("null");
                return;
            }
            final Class<?> type = value.getClass();
            if (type == Character.class) {
                final char c = (Character)value;
                if ((c >= (char)32) && (c <= (char)126)) {
                    out.append('\'').append(c).append('\'');
                } else {
                    out.append("(char)").append((int)c);
                }
            } else if (type.isEnum() || concreteValueClasses.contains(type)) {
                out.append(value);
            } else if (value instanceof CharSequence) {
                out.append('"').append(value).append('"');
            } else if (type.isArray()) {
                dumpArray(value);
            } else if (value instanceof Collection) {
                dumpCollection((Collection<?>)value);
            } else if (value instanceof Map) {
                dumpMap((Map<?, ?>)value);
            } else {
                dumpClass(type, value);
            }
        }

    }

}
