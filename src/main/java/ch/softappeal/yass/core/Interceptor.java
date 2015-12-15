package ch.softappeal.yass.core;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Intercepts a method invocation.
 */
@FunctionalInterface public interface Interceptor {

    /**
     * Performs extra treatments before and after the invocation.
     * @param arguments see {@link InvocationHandler#invoke(Object, Method, Object[])}
     * @return {@link Invocation#proceed()}
     * @throws Exception exception of {@link Invocation#proceed()}
     */
    @Nullable Object invoke(Method method, @Nullable Object[] arguments, Invocation invocation) throws Exception;

    /**
     * Calls {@link Invocation#proceed()}.
     */
    Interceptor DIRECT = (method, arguments, invocation) -> invocation.proceed();

    static Interceptor composite(final Interceptor interceptor1, final Interceptor interceptor2) {
        Check.notNull(interceptor1);
        Check.notNull(interceptor2);
        if (interceptor1 == DIRECT) {
            return interceptor2;
        }
        if (interceptor2 == DIRECT) {
            return interceptor1;
        }
        return (method, arguments, invocation) -> interceptor1.invoke(
            method,
            arguments,
            () -> interceptor2.invoke(method, arguments, invocation)
        );
    }

    static Interceptor composite(final Interceptor... interceptors) {
        Interceptor composite = DIRECT;
        for (final Interceptor interceptor : interceptors) {
            composite = composite(composite, interceptor);
        }
        return composite;
    }

    static Object invoke(final Interceptor interceptor, final Method method, final @Nullable Object[] arguments, final Object implementation) throws Exception {
        return interceptor.invoke(method, arguments, () -> {
            try {
                return method.invoke(implementation, arguments);
            } catch (final InvocationTargetException e) {
                try {
                    throw e.getCause();
                } catch (final Exception | Error e2) {
                    throw e2;
                } catch (final Throwable t) {
                    throw new Error(t);
                }
            }
        });
    }

    /**
     * @param <C> the contract type
     * @return a proxy for implementation using interceptors
     */
    @SuppressWarnings("unchecked")
    static <C> C proxy(final Class<C> contract, final C implementation, final Interceptor... interceptors) {
        Check.notNull(implementation);
        final Interceptor interceptor = composite(interceptors);
        if (interceptor == DIRECT) {
            Check.notNull(contract);
            return implementation;
        }
        return (C)Proxy.newProxyInstance(
            contract.getClassLoader(),
            new Class<?>[] {contract},
            (proxy, method, arguments) -> invoke(interceptor, method, arguments, implementation)
        );
    }

    /**
     * @param <T> the type of the {@link ThreadLocal}
     * @return an interceptor that changes threadLocal to value during {@link Interceptor#invoke(Method, Object[], Invocation)}
     */
    static <T> Interceptor threadLocal(final ThreadLocal<T> threadLocal, final @Nullable T value) {
        Check.notNull(threadLocal);
        return (method, arguments, invocation) -> {
            final @Nullable T oldValue = threadLocal.get();
            threadLocal.set(value);
            try {
                return invocation.proceed();
            } finally {
                threadLocal.set(oldValue);
            }
        };
    }

}
