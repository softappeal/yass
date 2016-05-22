package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reflector;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Reflect;
import ch.softappeal.yass.util.Tag;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This serializer assigns type and field id's from its {@link Tag}.
 */
public final class TaggedJsFastSerializer extends AbstractJsFastSerializer {

    private void addClass(final Class<?> type) {
        checkClass(type);
        final Map<Integer, Field> id2field = new HashMap<>(16);
        final Map<String, Field> name2field = new HashMap<>(16);
        for (final Field field : Reflect.allFields(type)) {
            final int id = Check.hasTag(field);
            final @Nullable Field oldFieldId = id2field.put(id, field);
            if (oldFieldId != null) {
                throw new IllegalArgumentException("tag " + id + " used for fields '" + field + "' and '" + oldFieldId + '\'');
            }
            final @Nullable Field oldFieldName = name2field.put(field.getName(), field);
            if (oldFieldName != null) {
                throw new IllegalArgumentException("duplicated fields '" + field + "' and '" + oldFieldName + "' in class hierarchy");
            }
        }
        addClass(Check.hasTag(type), type, false, id2field);
    }

    /**
     * @param concreteClasses instances of these classes can only be used in trees
     */
    public TaggedJsFastSerializer(
        final Reflector.Factory reflectorFactory,
        final Collection<TypeDesc> baseTypeDescs,
        final Collection<Class<?>> enumerations,
        final Collection<Class<?>> concreteClasses
    ) {
        super(reflectorFactory);
        baseTypeDescs.forEach(this::addBaseType);
        enumerations.forEach(type -> addEnum(Check.hasTag(type), type));
        concreteClasses.forEach(this::addClass);
        fixupFields();
    }

}
