package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps between {@link Method} and {@link Request#methodId}.
 */
public interface MethodMapper {


  /**
   * A {@link Method} mapping.
   */
  final class Mapping {

    public final Method method;

    /**
     * @see Request#methodId
     */
    public final Object methodId;

    /**
     * Oneway methods must 'return' void and must not throw any exceptions.
     */
    public final boolean oneWay;

    public Mapping(final Method method, final Object methodId, final boolean oneWay) {
      this.method = Check.notNull(method);
      this.methodId = Check.notNull(methodId);
      this.oneWay = oneWay;
    }

  }


  Mapping mapMethodId(Object methodId);

  Mapping mapMethod(Method method);


  /**
   * Creates a {@link MethodMapper} for a contract.
   */
  interface Factory {

    MethodMapper create(Class<?> contract);

  }


  /**
   * Uses {@link Method#getName()} as {@link Request#methodId}. Therefore, methods can't be overloaded!
   * Uses {@link OneWay} for marking oneway methods.
   */
  Factory STRING_FACTORY = new Factory() {

    @Override public MethodMapper create(final Class<?> contract) {
      final Method[] methods = contract.getMethods();
      final Map<String, Mapping> id2mapping = new HashMap<>(methods.length);
      for (final Method method : methods) {
        final String methodId = method.getName();
        if (id2mapping.put(methodId, new Mapping(method, methodId, method.getAnnotation(OneWay.class) != null)) != null) {
          throw new IllegalArgumentException("method '" + method + "' is overloaded");
        }
      }
      return new MethodMapper() {
        @Override public Mapping mapMethodId(final Object methodId) {
          return id2mapping.get(methodId);
        }
        @Override public Mapping mapMethod(final Method method) {
          return id2mapping.get(method.getName());
        }
      };
    }

  };


  /**
   * Sorts {@link Method#getName()} and uses {@link Integer} as {@link Request#methodId}. Therefore, methods can't be overloaded!
   * Uses {@link OneWay} for marking oneway methods.
   */
  final class IntegerFactory implements Factory {

    private static Mapping[] createMappings(final Class<?> contract) {
      final Method[] methods = contract.getMethods();
      Arrays.sort(methods, new Comparator<Method>() { // guarantees method order
        @Override public int compare(final Method m1, final Method m2) {
          return m1.getName().compareTo(m2.getName());
        }
      });
      final Mapping[] mappings = new Mapping[methods.length];
      final Set<String> names = new HashSet<>(methods.length);
      int id = 0;
      for (final Method method : methods) {
        final int methodId = id++;
        mappings[methodId] = new Mapping(method, methodId, method.getAnnotation(OneWay.class) != null);
        if (!names.add(method.getName())) {
          throw new IllegalArgumentException("method '" + method + "' is overloaded");
        }
      }
      return mappings;
    }

    @Override public MethodMapper create(final Class<?> contract) {
      final Mapping[] mappings = createMappings(contract);
      final Map<String, Mapping> name2mapping = new HashMap<>(mappings.length);
      for (final Mapping mapping : mappings) {
        name2mapping.put(mapping.method.getName(), mapping);
      }
      return new MethodMapper() {
        @Override public Mapping mapMethodId(final Object methodId) {
          return mappings[(Integer)methodId];
        }
        @Override public Mapping mapMethod(final Method method) {
          return name2mapping.get(method.getName());
        }
      };
    }

    public static void printNumbers(final PrintWriter printer, final Class<?> contract) {
      for (final Mapping mapping : createMappings(contract)) {
        printer.println(mapping.methodId + ": " + mapping.method.getName());
      }
    }

  }

  IntegerFactory INTEGER_FACTORY = new IntegerFactory();


  /**
   * Uses {@link Tag} as {@link Request#methodId}.
   * Uses {@link OneWay} for marking oneway methods.
   */
  Factory TAG_FACTORY = new Factory() {

    @Override public MethodMapper create(final Class<?> contract) {
      final Method[] methods = contract.getMethods();
      final Map<Integer, Mapping> tag2mapping = new HashMap<>(methods.length);
      for (final Method method : methods) {
        final int tag = Check.tag(method);
        if (tag2mapping.put(tag, new Mapping(method, tag, method.getAnnotation(OneWay.class) != null)) != null) {
          throw new IllegalArgumentException("tag '" + tag + "' of method '" + method + "' already used");
        }
      }
      return new MethodMapper() {
        @Override public Mapping mapMethodId(final Object methodId) {
          return tag2mapping.get(methodId);
        }
        @Override public Mapping mapMethod(final Method method) {
          return tag2mapping.get(method.getAnnotation(Tag.class).value());
        }
      };
    }

  };


}
