package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reflector;
import ch.softappeal.yass.util.Reflect;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This serializer assigns type and field id's automatically. Therefore, all peers must have the same version of the contract!
 */
public final class SimpleFastSerializer extends AbstractFastSerializer {

    private void addClass(final int typeId, final Class<?> type, final boolean referenceable) {
        checkClass(type);
        final Map<Integer, Field> id2field = new HashMap<>(16);
        int fieldId = FieldHandler.FIRST_ID;
        for (final Field field : Reflect.allFields(type)) {
            id2field.put(fieldId++, field);
        }
        addClass(typeId, type, referenceable, id2field);
    }

    /**
     * @param concreteClasses instances of these classes can only be used in trees
     * @param referenceableConcreteClasses instances of these classes can be used in graphs
     */
    public SimpleFastSerializer(
        final Reflector.Factory reflectorFactory,
        final Collection<BaseTypeHandler<?>> baseTypeHandlers,
        final Collection<Class<?>> enumerations,
        final Collection<Class<?>> concreteClasses,
        final Collection<Class<?>> referenceableConcreteClasses
    ) {
        super(reflectorFactory);
        int id = TypeDesc.FIRST_ID;
        for (final BaseTypeHandler<?> typeHandler : baseTypeHandlers) {
            addBaseType(new TypeDesc(id++, typeHandler));
        }
        for (final Class<?> type : enumerations) {
            addEnum(id++, type);
        }
        for (final Class<?> type : concreteClasses) {
            addClass(id++, type, false);
        }
        for (final Class<?> type : referenceableConcreteClasses) {
            addClass(id++, type, true);
        }
        fixupFields();
    }

}
