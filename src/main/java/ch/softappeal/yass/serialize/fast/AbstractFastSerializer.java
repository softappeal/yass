package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Reflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This fast and compact serializer supports the following types (type id's must be &gt;= {@link TypeDesc#FIRST_ID}):
 * <ul>
 * <li>null</li>
 * <li>{@link BaseTypeHandler} (see {@link BaseTypeHandlers} for default implementations)</li>
 * <li>{@link List} (deserialize creates an {@link ArrayList})</li>
 * <li>enumeration types (an enumeration constant is serialized with its ordinal number)</li>
 * <li>
 * class hierarchies with all non-static and non-transient fields
 * (field id's must be &gt;= {@link FieldHandler#FIRST_ID} and must be unique in the path to its super classes)
 * </li>
 * <li>exceptions (but without fields of {@link Throwable}; therefore, $note: you should implement {@link Throwable#getMessage()})</li>
 * <li>graphs with cycles</li>
 * </ul>
 * There is some support for contract versioning:
 * <ul>
 * <li>
 * Deserialization of old classes to new classes with new {@link Nullable} fields is allowed. These fields will be set to {@code null}.
 * Default values for these fields could be implemented with a getter method checking for {@code null}.
 * </li>
 * <li>
 * Deserialization of old enumerations to new enumerations with new constants at the end is allowed.
 * </li>
 * </ul>
 */
public abstract class AbstractFastSerializer implements Serializer {

    private final Reflector.Factory reflectorFactory;

    private Reflector reflector(final Class<?> type) {
        try {
            return reflectorFactory.create(type);
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    private final Map<Class<?>, TypeDesc> class2typeDesc = new HashMap<>(64);
    private final Map<Integer, TypeHandler> id2typeHandler = new HashMap<>(64);

    private void addType(final TypeDesc typeDesc) {
        if (class2typeDesc.put(typeDesc.handler.type, typeDesc) != null) {
            throw new IllegalArgumentException("type '" + typeDesc.handler.type.getCanonicalName() + "' already added");
        }
        final TypeHandler oldTypeHandler = id2typeHandler.put(typeDesc.id, typeDesc.handler);
        if (oldTypeHandler != null) {
            throw new IllegalArgumentException(
                "type id " + typeDesc.id + " used for '" + typeDesc.handler.type.getCanonicalName() + "' and '" +
                    oldTypeHandler.type.getCanonicalName() + '\''
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
        addType(new TypeDesc(id, new BaseTypeHandler<Enum<?>>(enumeration) {
            @Override public Enum<?> read(final Reader reader) throws Exception {
                return constants[reader.readVarInt()];
            }
            @Override public void write(final Enum<?> value, final Writer writer) throws Exception {
                writer.writeVarInt(value.ordinal());
            }
        }));
    }

    protected static void checkClass(final Class<?> type) {
        if (Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is abstract");
        } else if (type.isEnum()) {
            throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is an enumeration");
        }
    }

    protected final void addClass(final int id, final Class<?> type, final boolean referenceable, final Map<Integer, Field> id2field) {
        final Reflector reflector = reflector(type);
        final Map<Integer, FieldHandler> id2fieldHandler = new HashMap<>(id2field.size());
        id2field.forEach((fieldId, field) -> id2fieldHandler.put(fieldId, new FieldHandler(field, reflector.accessor(field))));
        addType(new TypeDesc(id, new ClassTypeHandler(type, reflector, referenceable, id2fieldHandler)));
    }

    protected final void addBaseType(final TypeDesc typeDesc) {
        if (typeDesc.handler.type.isEnum()) {
            throw new IllegalArgumentException("base type '" + typeDesc.handler.type.getCanonicalName() + "' is an enumeration");
        }
        addType(typeDesc);
    }

    protected AbstractFastSerializer(final Reflector.Factory reflectorFactory) {
        this.reflectorFactory = Check.notNull(reflectorFactory);
        addType(TypeDesc.NULL);
        addType(TypeDesc.REFERENCE);
        addType(TypeDesc.LIST);
    }

    protected final void fixupFields() {
        class2typeDesc.values().stream().filter(typeDesc -> typeDesc.handler instanceof ClassTypeHandler).forEach(
            typeDesc -> ((ClassTypeHandler)typeDesc.handler).fixupFields(class2typeDesc)
        );
    }

    @Override public final Object read(final Reader reader) throws Exception {
        return new Input(reader, id2typeHandler).read();
    }

    @Override public final void write(final Object value, final Writer writer) throws Exception {
        new Output(writer, class2typeDesc).write(value);
    }

    public final SortedMap<Integer, TypeHandler> id2typeHandler() {
        return new TreeMap<>(id2typeHandler);
    }

    public final void print(final PrintWriter printer) {
        id2typeHandler.forEach((id, typeHandler) -> {
            if (id >= TypeDesc.FIRST_ID) {
                printer.print(id + ": " + typeHandler.type.getCanonicalName());
                if (typeHandler instanceof BaseTypeHandler) {
                    final BaseTypeHandler<?> baseTypeHandler = (BaseTypeHandler<?>)typeHandler;
                    if (baseTypeHandler.type.isEnum()) {
                        printer.println();
                        final Object[] constants = baseTypeHandler.type.getEnumConstants();
                        for (int c = 0; c < constants.length; c++) {
                            printer.println("  " + c + ": " + ((Enum<?>)constants[c]).name());
                        }
                    } else {
                        printer.println();
                    }
                } else {
                    final ClassTypeHandler classTypeHandler = (ClassTypeHandler)typeHandler;
                    printer.println(" (referenceable=" + classTypeHandler.referenceable + ')');
                    for (final ClassTypeHandler.FieldDesc fieldDesc : classTypeHandler.fieldDescs()) {
                        printer.println("  " + fieldDesc.id + ": " + fieldDesc.handler.field);
                    }
                }
                printer.println();
            }
        });
    }

    public static List<Field> ownFields(final Class<?> type) {
        final List<Field> fields = new ArrayList<>(16);
        for (final Field field : type.getDeclaredFields()) {
            final int modifiers = field.getModifiers();
            if (!(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
                fields.add(field);
            }
        }
        return fields;
    }

    public static List<Field> allFields(Class<?> type) {
        final List<Field> fields = new ArrayList<>(16);
        while ((type != null) && (type != Throwable.class)) {
            fields.addAll(ownFields(type));
            type = type.getSuperclass();
        }
        return fields;
    }

}
