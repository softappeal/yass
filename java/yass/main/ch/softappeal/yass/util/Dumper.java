package ch.softappeal.yass.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Dumper {

    private static final Set<Class<?>> PRIMITIVE_WRAPPER_CLASSES = Set.of(
        Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class
    );

    private final boolean compact;
    private final boolean referenceables;

    private final Set<Class<?>> concreteValueClasses;
    protected final boolean isConcreteValueClass(final Class<?> type) {
        return concreteValueClasses.contains(type);
    }

    /**
     * @param compact one-liner or multiple lines
     * @param referenceables true: dumps graphs (objects are marked with #); false: dumps trees
     * @param concreteValueClasses only allowed if (referenceables); these objects should not reference others; do not print # for these classes
     */
    public Dumper(final boolean compact, final boolean referenceables, final Class<?>... concreteValueClasses) {
        this.compact = compact;
        this.referenceables = referenceables;
        if (!referenceables && (concreteValueClasses.length != 0)) {
            throw new IllegalArgumentException("concreteValueClasses only allowed if (referenceables)");
        }
        this.concreteValueClasses = Set.of(concreteValueClasses);
    }

    public final StringBuilder append(final StringBuilder s, final @Nullable Object value) {
        new Dump(s).dump(value);
        return s;
    }

    public final String dump(final @Nullable Object value) {
        return append(new StringBuilder(256), value).toString();
    }

    private final Map<Class<?>, List<Field>> class2fields = new ConcurrentHashMap<>();

    private final class Dump {
        private final StringBuilder out;
        Dump(final StringBuilder out) {
            this.out = Objects.requireNonNull(out);
        }
        private int tabs = 0;
        private void appendTabs() {
            for (var t = tabs; t > 0; t--) {
                out.append("    ");
            }
        }
        private void appendLine() {
            out.append('\n');
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
        private void dumpArray(final Object array) {
            final var length = Array.getLength(array);
            if (compact) {
                out.append("[ ");
                for (var i = 0; i < length; i++) {
                    dump(Array.get(array, i));
                    out.append(' ');
                }
                out.append(']');
            } else {
                inc("[");
                for (var i = 0; i < length; i++) {
                    appendTabs();
                    dump(Array.get(array, i));
                    appendLine();
                }
                dec("]");
            }
        }
        private void dumpCollection(final Collection<?> collection) {
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
        private void dumpMap(final Map<?, ?> map) {
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
        private void dumpClassFields(final List<Field> fields, final Object object) {
            for (final var field : fields) {
                final @Nullable Object value;
                try {
                    value = field.get(object);
                } catch (final IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
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
        private final @Nullable Map<Object, Integer> alreadyDumpedObjects = referenceables ? new IdentityHashMap<>(16) : null;
        private void dumpClass(final Class<?> type, final Object object) {
            final var referenceables = Dumper.this.referenceables && !isConcreteValueClass(type);
            final int index;
            if (referenceables) {
                final var reference = alreadyDumpedObjects.get(object);
                if (reference != null) {
                    out.append('#').append(reference);
                    return;
                }
                index = alreadyDumpedObjects.size();
                alreadyDumpedObjects.put(object, index);
            } else {
                index = 0;
            }
            if (!Dumper.this.dumpValueClass(out, type, object)) {
                final var fields = class2fields.computeIfAbsent(type, Reflect::allFields);
                if (compact) {
                    out.append(type.getSimpleName()).append("( ");
                    dumpClassFields(fields, object);
                    out.append(')');
                } else {
                    inc(type.getSimpleName() + '(');
                    dumpClassFields(fields, object);
                    dec(")");
                }
            }
            if (referenceables) {
                out.append('#').append(index);
            }
        }
        void dump(final @Nullable Object value) {
            if (value == null) {
                out.append("null");
            } else if (value instanceof CharSequence) {
                out.append('"').append(value).append('"');
            } else if (value instanceof Collection) {
                dumpCollection((Collection<?>)value);
            } else if (value instanceof Map) {
                dumpMap((Map<?, ?>)value);
            } else {
                final var type = value.getClass();
                if (type.isEnum() || PRIMITIVE_WRAPPER_CLASSES.contains(type)) {
                    out.append(value);
                } else if (type.isArray()) {
                    dumpArray(value);
                } else if (type == Character.class) {
                    out.append('\'').append(value).append('\'');
                } else {
                    dumpClass(type, value);
                }
            }
        }
    }

    /**
     * Could dump a value class (these should not reference other objects). Should be an one-liner.
     * This implementation does nothing and returns false.
     * @return true: if we dumped object of type to out; false: use default implementation
     */
    protected boolean dumpValueClass(final StringBuilder out, final Class<?> type, final Object object) {
        return false;
    }

}
