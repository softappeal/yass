package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;

import java.lang.reflect.Method;

/**
 * Maps between {@link Method} and {@link Request#methodId}.
 */
public interface MethodMapper {

    interface Factory {
        MethodMapper create(Class<?> contract);
    }

    final class Mapping {
        public final Method method;
        /**
         * @see Request#methodId
         */
        public final int id;
        /**
         * Oneway methods must 'return' void and must not throw exceptions.
         */
        public final boolean oneWay;
        public Mapping(final Method method, final int id, final boolean oneWay) {
            this.method = Check.notNull(method);
            this.id = id;
            this.oneWay = oneWay;
            if (oneWay) {
                if (method.getReturnType() != Void.TYPE) {
                    throw new IllegalArgumentException("oneway method '" + method + "' must 'return' void");
                }
                if (method.getExceptionTypes().length != 0) {
                    throw new IllegalArgumentException("oneway method '" + method + "' must not throw exceptions");
                }
            }
        }
        /**
         * Uses {@link OneWay} for marking oneway methods.
         */
        public Mapping(final Method method, final int id) {
            this(method, id, method.isAnnotationPresent(OneWay.class));
        }
    }

    Mapping mapId(int id);

    Mapping mapMethod(Method method);

}
