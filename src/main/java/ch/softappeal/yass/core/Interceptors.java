package ch.softappeal.yass.core;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * {@link Interceptor} utilities.
 */
public final class Interceptors {

    private Interceptors() {
        // disable
    }

    /**
     * Calls {@link Invocation#proceed()}.
     */
    public static final Interceptor DIRECT = new Interceptor() {
        @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
            return invocation.proceed();
        }
    };

    public static Interceptor composite(final Interceptor interceptor1, final Interceptor interceptor2) {
        Check.notNull(interceptor1);
        Check.notNull(interceptor2);
        if (interceptor1 == DIRECT) {
            return interceptor2;
        }
        if (interceptor2 == DIRECT) {
            return interceptor1;
        }
        return new Interceptor() {
            @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
                return interceptor1.invoke(
                    method,
                    arguments,
                    new Invocation() {
                        @Override public Object proceed() throws Throwable {
                            return interceptor2.invoke(method, arguments, invocation);
                        }
                    }
                );
            }
        };
    }

    public static Interceptor composite(final Interceptor... interceptors) {
        Interceptor composite = DIRECT;
        for (final Interceptor interceptor : interceptors) {
            composite = composite(composite, interceptor);
        }
        return composite;
    }

    /**
     * @param <C> the contract type
     * @return a proxy for implementation using interceptors
     */
    @SuppressWarnings("unchecked")
    public static <C> C proxy(final Class<C> contract, final C implementation, final Interceptor... interceptors) {
        Check.notNull(implementation);
        final Interceptor interceptor = composite(interceptors);
        if (interceptor == DIRECT) {
            Check.notNull(contract);
            return implementation;
        }
        return (C)Proxy.newProxyInstance(
            contract.getClassLoader(),
            new Class<?>[] {contract},
            new InvocationHandler() {
                @Override public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    return interceptor.invoke(method, arguments, new Invocation() {
                        @Override public Object proceed() throws Throwable {
                            try {
                                return method.invoke(implementation, arguments);
                            } catch (final InvocationTargetException e) {
                                throw e.getCause();
                            }
                        }
                    });
                }
            }
        );
    }

    /**
     * @param <T> the type of the {@link ThreadLocal}
     * @return an interceptor that changes threadLocal to value during {@link Interceptor#invoke(Method, Object[], Invocation)}
     */
    public static <T> Interceptor threadLocal(final ThreadLocal<T> threadLocal, @Nullable final T value) {
        Check.notNull(threadLocal);
        return new Interceptor() {
            @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
                @Nullable final T oldValue = threadLocal.get();
                threadLocal.set(value);
                try {
                    return invocation.proceed();
                } finally {
                    threadLocal.set(oldValue);
                }
            }
        };
    }

}
