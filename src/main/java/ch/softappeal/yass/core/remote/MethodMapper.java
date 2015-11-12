package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Maps between {@link Method} and {@link Request#methodId}.
 */
public interface MethodMapper {

    @FunctionalInterface interface Factory {
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
                    throw new IllegalArgumentException("oneWay method '" + method + "' must 'return' void");
                }
                if (method.getExceptionTypes().length != 0) {
                    throw new IllegalArgumentException("oneWay method '" + method + "' must not throw exceptions");
                }
            }
        }
        /**
         * Uses {@link OneWay} for marking oneWay methods.
         */
        public Mapping(final Method method, final int id) {
            this(method, id, method.isAnnotationPresent(OneWay.class));
        }
    }

    Mapping mapId(int id);

    Mapping mapMethod(Method method);

    static void print(final PrintWriter printer, final Factory factory, final Class<?> contract) {
        final MethodMapper methodMapper = factory.create(contract);
        Arrays.stream(contract.getMethods())
            .map(methodMapper::mapMethod)
            .sorted((mapping1, mapping2) -> ((Integer)mapping1.id).compareTo((Integer)mapping2.id))
            .forEach(mapping -> printer.println(mapping.id + ": " + mapping.method));
    }

}
