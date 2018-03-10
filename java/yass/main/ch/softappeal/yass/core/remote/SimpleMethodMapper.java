package ch.softappeal.yass.core.remote;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Sorts {@link Method#getName()} and uses index as {@link Request#methodId}. Therefore, methods can't be overloaded!
 * Uses {@link OneWay} for marking oneWay methods.
 */
public final class SimpleMethodMapper implements MethodMapper {

    private final Mapping[] mappings;
    private final Map<String, Mapping> name2mapping;

    private SimpleMethodMapper(final Class<?> contract) {
        final var methods = contract.getMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        mappings = new Mapping[methods.length];
        name2mapping = new HashMap<>(methods.length);
        var id = 0;
        for (final var method : methods) {
            final var mapping = new Mapping(method, id);
            final var oldMapping = name2mapping.put(method.getName(), mapping);
            if (oldMapping != null) {
                throw new IllegalArgumentException("methods '" + method + "' and '" + oldMapping.method + "' are overloaded");
            }
            mappings[id++] = mapping;
        }
    }

    @Override public Mapping mapId(final int id) {
        return mappings[id];
    }

    @Override public Mapping mapMethod(final Method method) {
        return name2mapping.get(method.getName());
    }

    public static final Factory FACTORY = SimpleMethodMapper::new;

}
