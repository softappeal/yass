package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class MethodMappers {


  private MethodMappers() {
    // disable
  }


  /**
   * Uses {@link Method#getName()} as {@link Request#methodId}. Therefore, methods can't be overloaded!
   * Uses {@link OneWay} for marking oneway methods.
   */
  public static final MethodMapper.Factory STRING_FACTORY = new MethodMapper.Factory() {

    @Override public MethodMapper create(final Class<?> contract) {
      final Method[] methods = contract.getMethods();
      final Map<String, MethodMapper.Mapping> id2mapping = new HashMap<>(methods.length);
      for (final Method method : methods) {
        final String id = method.getName();
        if (id2mapping.put(id, new MethodMapper.Mapping(method, id, method.getAnnotation(OneWay.class) != null)) != null) {
          throw new IllegalArgumentException("method '" + method + "' is overloaded");
        }
      }
      return new MethodMapper() {
        @Override public MethodMapper.Mapping mapId(final Object id) {
          return id2mapping.get(id);
        }
        @Override public MethodMapper.Mapping mapMethod(final Method method) {
          return id2mapping.get(method.getName());
        }
      };
    }

  };


  /**
   * Uses {@link Tag} as {@link Request#methodId}.
   * Uses {@link OneWay} for marking oneway methods.
   */
  public static final MethodMapper.Factory TAG_FACTORY = new MethodMapper.Factory() {

    @Override public MethodMapper create(final Class<?> contract) {
      final Method[] methods = contract.getMethods();
      final Map<Integer, MethodMapper.Mapping> id2mapping = new HashMap<>(methods.length);
      for (final Method method : methods) {
        final int id = Check.tag(method);
        if (id2mapping.put(id, new MethodMapper.Mapping(method, id, method.getAnnotation(OneWay.class) != null)) != null) {
          throw new IllegalArgumentException("tag '" + id + "' of method '" + method + "' already used");
        }
      }
      return new MethodMapper() {
        @Override public MethodMapper.Mapping mapId(final Object id) {
          return id2mapping.get(id);
        }
        @Override public MethodMapper.Mapping mapMethod(final Method method) {
          return id2mapping.get(method.getAnnotation(Tag.class).value());
        }
      };
    }

  };


}
