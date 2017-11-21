package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Nullable;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This fast and compact serializer supports the following types (type id's must be &gt;= {@link TypeDesc#FIRST_ID}):
 * <ul>
 * <li>null</li>
 * <li>{@link BaseTypeHandler} (see {@link BaseTypeHandlers} for default implementations)</li>
 * <li>{@link List} (deserialize creates an {@link ArrayList})</li>
 * <li>enumeration types (an enumeration constant is serialized with its ordinal number)</li>
 * <li>
 * class hierarchies with all non-static and non-transient fields
 * (field names and id's must be unique in the path to its super classes and id's must be &gt;= {@link FieldHandler#FIRST_ID})
 * </li>
 * <li>exceptions (but without fields of {@link Throwable}; therefore, you should implement {@link Throwable#getMessage()})</li>
 * <li>graphs with cycles</li>
 * </ul>
 * There is some support for contract versioning:
 * <ul>
 * <li>
 * Deserialization of old classes to new classes with new {@link Nullable} fields is allowed.
 * These fields will be set to {@code null} (ignoring constructors).
 * Default values for these fields could be implemented with a getter method checking for {@code null}.
 * </li>
 * <li>
 * Serialization of new classes with new {@link Nullable} fields to old classes is allowed if the new values are {@code null}.
 * </li>
 * <li>
 * Deserialization of old enumerations to new enumerations with new constants at the end is allowed.
 * </li>
 * </ul>
 */
public abstract class FastSerializer implements Serializer {

    private final Function<Class<?>, Supplier<Object>> instantiators;
    private final Map<Class<?>, TypeDesc> class2typeDesc = new HashMap<>(64);
    private final Map<Integer, TypeHandler> id2typeHandler = new HashMap<>(64);

    private void addType(final TypeDesc typeDesc) {
        if (class2typeDesc.put(typeDesc.handler.type, typeDesc) != null) {
            throw new IllegalArgumentException("type '" + typeDesc.handler.type.getCanonicalName() + "' already added");
        }
        final @Nullable TypeHandler oldTypeHandler = id2typeHandler.put(typeDesc.id, typeDesc.handler);
        if (oldTypeHandler != null) {
            throw new IllegalArgumentException(
                "type id " + typeDesc.id + " used for '" + typeDesc.handler.type.getCanonicalName() + "' and '" + oldTypeHandler.type.getCanonicalName() + '\''
            );
        }
    }

    @SuppressWarnings("unchecked")
    protected final void addEnum(final int id, final Class<?> type) {
        if (!type.isEnum()) {
            throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is not an enumeration");
        }
        final Class<Enum<?>> enumeration = (Class<Enum<?>>)type;
        final Enum<?>[] constants = enumeration.getEnumConstants();
        addType(new TypeDesc(id, new BaseTypeHandler<>(enumeration) {
            @Override public Enum<?> read(final Reader reader) throws Exception {
                return constants[reader.readVarInt()];
            }
            @Override public void write(final Enum<?> value, final Writer writer) throws Exception {
                writer.writeVarInt(value.ordinal());
            }
        }));
    }

    protected final void checkClass(final Class<?> type) {
        if (type.isEnum()) {
            throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is an enumeration");
        }
    }

    protected final void addClass(final int id, final Class<?> type, final boolean referenceable, final Map<Integer, Field> id2field) {
        if (Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is abstract");
        }
        final Map<Integer, FieldHandler> id2fieldHandler = new HashMap<>(id2field.size());
        final Map<String, Field> name2field = new HashMap<>(id2field.size());
        id2field.forEach((fieldId, field) -> {
            final @Nullable Field oldField = name2field.put(field.getName(), field);
            if (oldField != null) { // reason: too confusing
                throw new IllegalArgumentException("duplicated field name '" + field + "' and '" + oldField + "' not allowed in class hierarchy");
            }
            id2fieldHandler.put(fieldId, new FieldHandler(field));
        });
        addType(new TypeDesc(id, new ClassTypeHandler(type, instantiators, referenceable, id2fieldHandler)));
    }

    protected final void addBaseType(final TypeDesc typeDesc) {
        if (typeDesc.handler.type.isEnum()) {
            throw new IllegalArgumentException("base type '" + typeDesc.handler.type.getCanonicalName() + "' is an enumeration");
        }
        addType(typeDesc);
    }

    protected FastSerializer(final Function<Class<?>, Supplier<Object>> instantiators) {
        this.instantiators = Objects.requireNonNull(instantiators);
        addType(TypeDesc.NULL);
        addType(TypeDesc.REFERENCE);
        addType(TypeDesc.LIST);
    }

    protected final void fixupFields() {
        class2typeDesc.values().stream().filter(typeDesc -> typeDesc.handler instanceof ClassTypeHandler).forEach(
            typeDesc -> ((ClassTypeHandler)typeDesc.handler).fixupFields(class2typeDesc)
        );
    }

    @Override public final @Nullable Object read(final Reader reader) throws Exception {
        return new Input(reader, id2typeHandler).read();
    }

    @Override public final void write(final @Nullable Object value, final Writer writer) throws Exception {
        new Output(writer, class2typeDesc).write(value);
    }

    public final SortedMap<Integer, TypeHandler> id2typeHandler() {
        return new TreeMap<>(id2typeHandler);
    }

    public final void print(final PrintWriter printer) {
        id2typeHandler().forEach((id, typeHandler) -> {
            if (id >= TypeDesc.FIRST_ID) {
                printer.print(id + ": " + typeHandler.type.getCanonicalName());
                if (typeHandler instanceof BaseTypeHandler) {
                    printer.println();
                    final Class<?> type = ((BaseTypeHandler<?>)typeHandler).type;
                    if (type.isEnum()) {
                        final Object[] constants = type.getEnumConstants();
                        for (int c = 0; c < constants.length; c++) {
                            printer.println("    " + c + ": " + ((Enum<?>)constants[c]).name());
                        }
                    }
                } else {
                    final ClassTypeHandler classTypeHandler = (ClassTypeHandler)typeHandler;
                    printer.println(" (referenceable=" + classTypeHandler.referenceable + ')');
                    for (final ClassTypeHandler.FieldDesc fieldDesc : classTypeHandler.fieldDescs()) {
                        printer.println("    " + fieldDesc.id + ": " + fieldDesc.handler.field.toGenericString());
                    }
                }
                printer.println();
            }
        });
    }

}
