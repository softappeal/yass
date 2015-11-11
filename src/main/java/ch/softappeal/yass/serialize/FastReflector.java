package ch.softappeal.yass.serialize;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Reflect;

import java.lang.reflect.Field;

/**
 * Even works if classes don't have a default constructor; uses {@link Reflect#UNSAFE}.
 */
public final class FastReflector implements Reflector {

    public static final Factory FACTORY = FastReflector::new;

    private FastReflector(final Class<?> type) {
        this.type = Check.notNull(type);
    }

    private final Class<?> type;

    @Override public Object newInstance() throws InstantiationException {
        return Reflect.UNSAFE.allocateInstance(type);
    }

    @Override public Accessor accessor(final Field field) {
        final Class<?> type = field.getType();
        final long offset = Reflect.UNSAFE.objectFieldOffset(field);
        if (!type.isPrimitive()) {
            return new Accessor() {
                @Override public Object get(final Object object) {
                    return Reflect.UNSAFE.getObject(object, offset);
                }
                @Override public void set(final Object object, final @Nullable Object value) {
                    Reflect.UNSAFE.putObject(object, offset, value);
                }
            };
        }
        if (type == Boolean.TYPE) {
            return new Accessor() {
                @Override public Object get(final Object object) {
                    return Reflect.UNSAFE.getBoolean(object, offset);
                }
                @Override public void set(final Object object, final @Nullable Object value) {
                    Reflect.UNSAFE.putBoolean(object, offset, (Boolean)value);
                }
            };
        }
        if (type == Character.TYPE) {
            return new Accessor() {
                @Override public Object get(final Object object) {
                    return Reflect.UNSAFE.getChar(object, offset);
                }
                @Override public void set(final Object object, final @Nullable Object value) {
                    Reflect.UNSAFE.putChar(object, offset, (Character)value);
                }
            };
        }
        if (type == Byte.TYPE) {
            return new Accessor() {
                @Override public Object get(final Object object) {
                    return Reflect.UNSAFE.getByte(object, offset);
                }
                @Override public void set(final Object object, final @Nullable Object value) {
                    Reflect.UNSAFE.putByte(object, offset, (Byte)value);
                }
            };
        }
        if (type == Short.TYPE) {
            return new Accessor() {
                @Override public Object get(final Object object) {
                    return Reflect.UNSAFE.getShort(object, offset);
                }
                @Override public void set(final Object object, final @Nullable Object value) {
                    Reflect.UNSAFE.putShort(object, offset, (Short)value);
                }
            };
        }
        if (type == Integer.TYPE) {
            return new Accessor() {
                @Override public Object get(final Object object) {
                    return Reflect.UNSAFE.getInt(object, offset);
                }
                @Override public void set(final Object object, final @Nullable Object value) {
                    Reflect.UNSAFE.putInt(object, offset, (Integer)value);
                }
            };
        }
        if (type == Long.TYPE) {
            return new Accessor() {
                @Override public Object get(final Object object) {
                    return Reflect.UNSAFE.getLong(object, offset);
                }
                @Override public void set(final Object object, final @Nullable Object value) {
                    Reflect.UNSAFE.putLong(object, offset, (Long)value);
                }
            };
        }
        if (type == Float.TYPE) {
            return new Accessor() {
                @Override public Object get(final Object object) {
                    return Reflect.UNSAFE.getFloat(object, offset);
                }
                @Override public void set(final Object object, final @Nullable Object value) {
                    Reflect.UNSAFE.putFloat(object, offset, (Float)value);
                }
            };
        }
        if (type == Double.TYPE) {
            return new Accessor() {
                @Override public Object get(final Object object) {
                    return Reflect.UNSAFE.getDouble(object, offset);
                }
                @Override public void set(final Object object, final @Nullable Object value) {
                    Reflect.UNSAFE.putDouble(object, offset, (Double)value);
                }
            };
        }
        throw new RuntimeException("unexpected type " + type);
    }

}
