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
   * @param <C> the contract type
   * @return a proxy for implementation using interceptors
   */
  @SuppressWarnings("WeakerAccess")
  public static <C> C proxy(final Class<C> contract, final C implementation, final Interceptor... interceptors) {
    Check.notNull(implementation);
    final Interceptor interceptor = composite(interceptors);
    return contract.cast(Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[] {contract}, new InvocationHandler() {
      @Override public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
        return interceptor.invoke(method, arguments, new Invocation() {
          @Override public Object proceed() throws Throwable {
            try {
              return method.invoke(implementation, arguments);
            } catch (final InvocationTargetException e) {
              //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
              throw e.getCause();
            }
          }
        });
      }
    }));
  }


  /**
   * Calls {@link Invocation#proceed()}.
   */
  public static final Interceptor DIRECT = new Interceptor() {
    @Override @Nullable public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
      return invocation.proceed();
    }
  };


  @SuppressWarnings("ObjectEquality")
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
      @Override @Nullable public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
        return interceptor1.invoke(method, arguments, new Invocation() {
          @Override @Nullable public Object proceed() throws Throwable {
            return interceptor2.invoke(method, arguments, invocation);
          }
        });
      }
    };
  }

  /**
   * @return an interceptor that is a composite of interceptors
   */
  public static Interceptor composite(final Interceptor... interceptors) {
    Interceptor compositeInterceptor = DIRECT;
    for (final Interceptor interceptor : interceptors) {
      compositeInterceptor = composite(compositeInterceptor, interceptor);
    }
    return compositeInterceptor;
  }


  /**
   * @param <T> the type of the {@link ThreadLocal}
   * @return an interceptor that changes threadLocal to value during {@link Interceptor#invoke(Method, Object[], Invocation)}
   */
  public static <T> Interceptor threadLocal(final ThreadLocal<T> threadLocal, final T value) {
    Check.notNull(threadLocal);
    Check.notNull(value);
    return new Interceptor() {
      @Override @Nullable public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
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

  /**
   * @return is there an active invocation ?
   */
  public static boolean hasInvocation(final ThreadLocal<?> threadLocal) {
    return threadLocal.get() != null;
  }

  /**
   * @return {@link ThreadLocal#get()} of the active invocation
   * @throws IllegalStateException if there is no active invocation
   * @see #hasInvocation(ThreadLocal)
   */
  public static <T> T getInvocation(final ThreadLocal<T> threadLocal) throws IllegalStateException {
    @Nullable final T value = threadLocal.get();
    if (value == null) {
      throw new IllegalStateException("no active invocation");
    }
    return value;
  }


}
