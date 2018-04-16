package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.Nullable;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FieldHandler {

    static final int END_ID = 0;
    public static final int FIRST_ID = END_ID + 1;

    public final Field field;

    private @Nullable TypeHandler typeHandler;
    /**
     * note: !{@link Optional#isPresent()} if {@link ClassTypeHandler} or type not in class2typeDesc (Object, Throwable, abstract classes, ...).
     */
    public Optional<TypeHandler> typeHandler() {
        return Optional.ofNullable(typeHandler);
    }

    FieldHandler(final Field field) {
        this.field = Objects.requireNonNull(field);
    }

    void fixup(final Map<Class<?>, TypeDesc> class2typeDesc) {
        final var typeDesc = class2typeDesc.get(
            primitiveWrapperType(field.getType()) // note: prevents that primitive types are written with type id
        );
        typeHandler = (typeDesc == null) ? null : typeDesc.handler;
        if (typeHandler instanceof ClassTypeHandler) {
            typeHandler = null;
        }
    }

    void read(final Object object, final Input input) throws Exception {
        field.set(object, (typeHandler == null) ? input.read() : typeHandler.read(input));
    }

    /**
     * @see ClassTypeHandler#read(Input)
     */
    void write(final int id, final Object object, final Output output) throws Exception {
        final var value = field.get(object);
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
        } else if (type == byte.class) {
            return Byte.class;
        } else if (type == short.class) {
            return Short.class;
        } else if (type == int.class) {
            return Integer.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == char.class) {
            return Character.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == double.class) {
            return Double.class;
        }
        return type;
    }

}
