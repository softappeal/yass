package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;

import java.lang.reflect.Method;

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
    public final Object id;

    /**
     * Oneway methods must 'return' void and must not throw any exceptions.
     */
    public final boolean oneWay;

    public Mapping(final Method method, final Object id, final boolean oneWay) {
      this.method = Check.notNull(method);
      this.id = Check.notNull(id);
      this.oneWay = oneWay;
      if (oneWay) {
        if (method.getReturnType() != Void.TYPE) {
          throw new IllegalArgumentException("oneway method '" + method + "' must return 'void'");
        }
        if (method.getExceptionTypes().length != 0) {
          throw new IllegalArgumentException("oneway method '" + method + "' must not throw exceptions");
        }
      }
    }

    public Mapping(final Method method, final Object id) {
      this(method, id, method.getAnnotation(OneWay.class) != null);
    }

  }


  Mapping mapId(Object id);

  Mapping mapMethod(Method method);


  /**
   * Creates a {@link MethodMapper} for a contract.
   */
  @FunctionalInterface
  interface Factory {

    MethodMapper create(Class<?> contract);

  }


}
