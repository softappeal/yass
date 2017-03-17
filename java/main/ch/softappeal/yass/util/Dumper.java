package ch.softappeal.yass.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Dumper {

    private static final Set<Class<?>> PRIMITIVE_WRAPPER_CLASSES = new HashSet<>(Arrays.asList(
        Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class
    ));

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
        this.concreteValueClasses = new HashSet<>(Arrays.asList(concreteValueClasses));
    }

    public final StringBuilder append(final StringBuilder s, final @Nullable Object value) {
        new Dump(s).dump(value);
        return s;
    }

    private static final class FieldDesc {
        final String name;
        final Function<Object, Object> accessor;
        FieldDesc(final Field field) {
            name = field.getName();
            final Class<?> type = field.getType();
            final long offset = Reflect.UNSAFE.objectFieldOffset(field);
            if (!type.isPrimitive()) {
                accessor = object -> Reflect.UNSAFE.getObject(object, offset);
            } else if (type == Boolean.TYPE) {
                accessor = object -> Reflect.UNSAFE.getBoolean(object, offset);
            } else if (type == Character.TYPE) {
                accessor = object -> Reflect.UNSAFE.getChar(object, offset);
            } else if (type == Byte.TYPE) {
                accessor = object -> Reflect.UNSAFE.getByte(object, offset);
            } else if (type == Short.TYPE) {
                accessor = object -> Reflect.UNSAFE.getShort(object, offset);
            } else if (type == Integer.TYPE) {
                accessor = object -> Reflect.UNSAFE.getInt(object, offset);
            } else if (type == Long.TYPE) {
                accessor = object -> Reflect.UNSAFE.getLong(object, offset);
            } else if (type == Float.TYPE) {
                accessor = object -> Reflect.UNSAFE.getFloat(object, offset);
            } else if (type == Double.TYPE) {
                accessor = object -> Reflect.UNSAFE.getDouble(object, offset);
            } else {
                throw new RuntimeException("unexpected type " + type);
            }
        }
    }

    private final Map<Class<?>, List<FieldDesc>> class2fieldDescs = new ConcurrentHashMap<>();

    private final class Dump {
        private final StringBuilder out;
        Dump(final StringBuilder out) {
            this.out = Objects.requireNonNull(out);
        }
        private int tabs = 0;
        private void appendTabs() {
            for (int t = tabs; t > 0; t--) {
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
        private void dumpClassFields(final List<FieldDesc> fieldDescs, final Object object) {
            for (final FieldDesc fieldDesc : fieldDescs) {
                final @Nullable Object value = fieldDesc.accessor.apply(object);
                if (value != null) {
                    if (compact) {
                        out.append(fieldDesc.name).append('=');
                        dump(value);
                        out.append(' ');
                    } else {
                        appendTabs();
                        out.append(fieldDesc.name).append(" = ");
                        dump(value);
                        appendLine();
                    }
                }
            }
        }
        private final @Nullable Map<Object, Integer> alreadyDumpedObjects = referenceables ? new IdentityHashMap<>(16) : null;
        private void dumpClass(final Class<?> type, final Object object) {
            final boolean referenceables = Dumper.this.referenceables && !isConcreteValueClass(type);
            final int index;
            if (referenceables) {
                final @Nullable Integer reference = alreadyDumpedObjects.get(object);
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
                final List<FieldDesc> fieldDescs = class2fieldDescs.computeIfAbsent(
                    type,
                    t -> Reflect.allFields(t).stream().map(FieldDesc::new).collect(Collectors.toList())
                );
                if (compact) {
                    out.append(type.getSimpleName()).append("( ");
                    dumpClassFields(fieldDescs, object);
                    out.append(')');
                } else {
                    inc(type.getSimpleName() + '(');
                    dumpClassFields(fieldDescs, object);
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
                final Class<?> type = value.getClass();
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
