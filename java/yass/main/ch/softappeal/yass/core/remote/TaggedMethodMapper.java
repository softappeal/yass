package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;
import ch.softappeal.yass.util.Tags;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses {@link Tag} as {@link Request#methodId}.
 * Uses {@link OneWay} for marking oneWay methods.
 */
public final class TaggedMethodMapper implements MethodMapper {

    private final Map<Integer, Mapping> id2mapping;

    private TaggedMethodMapper(final Class<?> contract) {
        final Method[] methods = contract.getMethods();
        id2mapping = new HashMap<>(methods.length);
        for (final Method method : methods) {
            final int id = Tags.getTag(method);
            final @Nullable Mapping oldMapping = id2mapping.put(id, new Mapping(method, id));
            if (oldMapping != null) {
                throw new IllegalArgumentException("tag " + id + " used for methods '" + method + "' and '" + oldMapping.method + '\'');
            }
        }
    }

    @Override public Mapping mapId(final int id) {
        return id2mapping.get(id);
    }

    @Override public Mapping mapMethod(final Method method) {
        return id2mapping.get(Tags.getTag(method));
    }

    public static final Factory FACTORY = TaggedMethodMapper::new;

}
