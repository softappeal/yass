package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reflector;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Field;
import java.util.Map;

public final class FieldHandler {

    static final int END_ID = 0;
    public static final int FIRST_ID = END_ID + 1;

    public final Field field;
    private final Reflector.Accessor accessor;

    private @Nullable TypeHandler typeHandler;
    /**
     * Note: null if {@link ClassTypeHandler} or type not in class2typeDesc (Object, Throwable, abstract classes, ...).
     */
    public @Nullable TypeHandler typeHandler() {
        return typeHandler;
    }

    FieldHandler(final Field field, final Reflector.Accessor accessor) {
        this.field = Check.notNull(field);
        this.accessor = Check.notNull(accessor);
    }

    void fixup(final Map<Class<?>, TypeDesc> class2typeDesc) {
        final TypeDesc typeDesc = class2typeDesc.get(
            primitiveWrapperType(field.getType()) // note: prevents that primitive types are written with type id
        );
        typeHandler = (typeDesc == null) ? null : typeDesc.handler;
        if (typeHandler instanceof ClassTypeHandler) {
            typeHandler = null;
        }
    }

    void read(final Object object, final Input input) throws Exception {
        accessor.set(object, (typeHandler == null) ? input.read() : typeHandler.read(input));
    }

    /**
     * @see ClassTypeHandler#read(Input)
     */
    void write(final int id, final Object object, final Output output) throws Exception {
        final Object value = accessor.get(object);
        if (value != null) {
            output.writer.writeVarInt(id);
            if (typeHandler == null) {
                output.write(value);
            } else {
                typeHandler.write(value, output);
            }
        }
    }

    public static Class<?> primitiveWrapperType(final Class<?> type) {
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        return type;
    }

}
