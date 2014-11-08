package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reflector;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This serializer assigns type and field id's from its {@link Tag}.
 */
public final class TaggedFastSerializer extends AbstractFastSerializer {

    private void addClass(final Class<?> type, final boolean referenceable) {
        checkClass(type);
        final Map<Integer, Field> id2field = new HashMap<>(16);
        for (final Field field : allFields(type)) {
            final int id = Check.hasTag(field);
            final Field oldField = id2field.put(id, field);
            if (oldField != null) {
                throw new IllegalArgumentException("tag " + id + " used for fields '" + field + "' and '" + oldField + '\'');
            }
        }
        addClass(Check.hasTag(type), type, referenceable, id2field);
    }

    /**
     * @param concreteClasses instances of these classes can only be used in trees
     * @param referenceableConcreteClasses instances of these classes can be used in graphs
     */
    public TaggedFastSerializer(
        final Reflector.Factory reflectorFactory, final Collection<TypeDesc> baseTypeDescs, final Collection<Class<?>> enumerations,
        final Collection<Class<?>> concreteClasses, final Collection<Class<?>> referenceableConcreteClasses
    ) {
        super(reflectorFactory);
        baseTypeDescs.forEach(this::addBaseType);
        enumerations.forEach(type -> addEnum(Check.hasTag(type), type));
        concreteClasses.forEach(type -> addClass(type, false));
        referenceableConcreteClasses.forEach(type -> addClass(type, true));
        fixupFields();
    }

}
