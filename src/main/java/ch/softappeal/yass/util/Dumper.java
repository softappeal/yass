package ch.softappeal.yass.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class Dumper {

    private static final Set<Class<?>> PRIMITIVE_WRAPPER_CLASSES = new HashSet<>(Arrays.asList(
        Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class
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

    public StringBuilder append(final StringBuilder s, final @Nullable Object value) {
        new Dump(s).dump(value);
        return s;
    }

    @FunctionalInterface private interface Accessor {
        @Nullable Object get(Object object);
    }

    private static final class FieldDesc {
        final String name;
        final Accessor accessor;
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

    private static final class ClassDesc {
        final boolean noToString;
        final List<FieldDesc> fieldDescs;
        ClassDesc(final Class<?> type) {
            try {
                final Class<?> toStringClass = type.getMethod("toString").getDeclaringClass();
                noToString = (toStringClass == Object.class) || (toStringClass == Throwable.class);
                fieldDescs = Reflect.allFields(type).stream().map(FieldDesc::new).collect(Collectors.toList());
            } catch (final NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final Map<Class<?>, ClassDesc> class2desc = new ConcurrentHashMap<>();

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
        private void dumpClassFields(final ClassDesc classDesc, final Object object) {
            for (final FieldDesc fieldDesc : classDesc.fieldDescs) {
                final @Nullable Object value = fieldDesc.accessor.get(object);
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
        private final Map<Object, Integer> alreadyDumpedObjects = cycles ? new IdentityHashMap<>(16) : null;
        private void dumpClass(final Class<?> type, final Object object) {
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
            @Nullable ClassDesc classDesc = class2desc.get(type);
            if (classDesc == null) {
                classDesc = new ClassDesc(type);
                class2desc.put(type, classDesc);
            }
            if (classDesc.noToString) {
                if (compact) {
                    out.append(type.getSimpleName()).append("( ");
                    dumpClassFields(classDesc, object);
                    out.append(')');
                } else {
                    inc(type.getSimpleName() + '(');
                    dumpClassFields(classDesc, object);
                    dec(")");
                }
            } else {
                out.append(object);
            }
            if (cycles) {
                out.append('#').append(index);
            }
        }
        void dump(final @Nullable Object value) {
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
