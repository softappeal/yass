package ch.softappeal.yass.core.remote;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
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
    Arrays.sort(methods, new Comparator<Method>() {
      @Override public int compare(final Method method1, final Method method2) {
        return method1.getName().compareTo(method2.getName());
      }
    });
    mappings = new Mapping[methods.length];
    name2mapping = new HashMap<>(methods.length);
    int id = 0;
    for (final Method method : methods) {
      final Mapping mapping = new Mapping(method, id, method.getAnnotation(OneWay.class) != null);
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

  public static final Factory FACTORY = new Factory() {
    @Override public MethodMapper create(final Class<?> contract) {
      return new SimpleMethodMapper(contract);
    }
  };

}
