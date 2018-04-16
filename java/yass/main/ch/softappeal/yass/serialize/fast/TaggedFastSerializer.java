package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.Reflect;
import ch.softappeal.yass.Tag;
import ch.softappeal.yass.Tags;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This serializer assigns type and field id's from its {@link Tag}.
 */
public final class TaggedFastSerializer extends FastSerializer {

    private void addClass(final Class<?> type, final boolean referenceable) {
        final Map<Integer, Field> id2field = new HashMap<>(16);
        for (final var field : Reflect.allFields(type)) {
            final var id = Tags.getTag(field);
            final var oldField = id2field.put(id, field);
            if (oldField != null) {
                throw new IllegalArgumentException("tag " + id + " used for fields '" + field + "' and '" + oldField + '\'');
            }
        }
        addClass(Tags.getTag(type), type, referenceable, id2field);
    }

    /**
     * @param concreteClasses instances of these classes can only be used in trees
     * @param referenceableConcreteClasses instances of these classes can be used in graphs
     */
    public TaggedFastSerializer(final Collection<TypeDesc> baseTypeDescs, final Collection<Class<?>> concreteClasses, final Collection<Class<?>> referenceableConcreteClasses) {
        baseTypeDescs.forEach(this::addBaseType);
        concreteClasses.forEach(type -> {
            if (type.isEnum()) {
                addEnum(Tags.getTag(type), type);
            } else {
                addClass(type, false);
            }
        });
        referenceableConcreteClasses.forEach(type -> {
            checkClass(type);
            addClass(type, true);
        });
        fixupFields();
    }

    public TaggedFastSerializer(final Collection<TypeDesc> baseTypeDescs, final Collection<Class<?>> concreteClasses) {
        this(baseTypeDescs, concreteClasses, List.of());
    }

}
