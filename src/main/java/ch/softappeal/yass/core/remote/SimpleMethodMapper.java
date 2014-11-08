package ch.softappeal.yass.core.remote;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Sorts {@link Method#getName()} and uses index as {@link Request#methodId}. Therefore, methods can't be overloaded!
 * Uses {@link OneWay} for marking oneway methods.
 */
public final class SimpleMethodMapper implements MethodMapper {

    private final Mapping[] mappings;
    private final Map<String, Mapping> name2mapping;

    private SimpleMethodMapper(final Class<?> contract) {
        final Method[] methods = contract.getMethods();
        Arrays.sort(methods, (method1, method2) -> method1.getName().compareTo(method2.getName()));
        mappings = new Mapping[methods.length];
        name2mapping = new HashMap<>(methods.length);
        int id = 0;
        for (final Method method : methods) {
            final Mapping mapping = new Mapping(method, id);
            final Mapping oldMapping = name2mapping.put(method.getName(), mapping);
            if (oldMapping != null) {
                throw new IllegalArgumentException("methods '" + method + "' and '" + oldMapping.method + "' are overloaded");
            }
            mappings[id++] = mapping;
        }
    }

    @Override public Mapping mapId(final Object id) {
        return mappings[(Integer)id];
    }

    @Override public Mapping mapMethod(final Method method) {
        return name2mapping.get(method.getName());
    }

    public static final Factory FACTORY = SimpleMethodMapper::new;

}
