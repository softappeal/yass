package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reflector;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Reflect;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This serializer assigns type and field id's automatically. Therefore, all peers must have the same version of the contract!
 */
public final class SimpleJsFastSerializer extends AbstractJsFastSerializer {

    private void addClass(final int typeId, final Class<?> type) {
        checkClass(type);
        final List<Field> fields = Reflect.allFields(type);
        final Map<String, Field> name2field = new HashMap<>(fields.size());
        for (final Field field : fields) {
            final @Nullable Field oldField = name2field.put(field.getName(), field);
            if (oldField != null) {
                throw new IllegalArgumentException("duplicated fields '" + field + "' and '" + oldField + "' in class hierarchy");
            }
        }
        final Map<Integer, Field> id2field = new HashMap<>(fields.size());
        int fieldId = FieldHandler.FIRST_ID;
        for (final Field field : fields) {
            id2field.put(fieldId++, field);
        }
        addClass(typeId, type, false, id2field);
    }

    /**
     * @param concreteClasses instances of these classes can only be used in trees
     */
    public SimpleJsFastSerializer(
        final Reflector.Factory reflectorFactory,
        final Collection<BaseTypeHandler<?>> baseTypeHandlers,
        final Collection<Class<?>> enumerations,
        final Collection<Class<?>> concreteClasses
    ) {
        super(reflectorFactory);
        int id = FIRST_DESC_ID;
        for (final BaseTypeHandler<?> typeHandler : baseTypeHandlers) {
            addBaseType(new TypeDesc(id++, typeHandler));
        }
        for (final Class<?> type : enumerations) {
            addEnum(id++, type);
        }
        for (final Class<?> type : concreteClasses) {
            addClass(id++, type);
        }
        fixupFields();
    }

}
