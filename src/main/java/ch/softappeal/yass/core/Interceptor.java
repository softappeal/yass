package ch.softappeal.yass.core;

import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Method;

/**
 * Intercepts a method invocation.
 * @see Interceptors
 */
@FunctionalInterface
public interface Interceptor {

  /**
   * Performs extra treatments before and after the invocation.
   * @return {@link Invocation#proceed()}
   * @throws Throwable exception of {@link Invocation#proceed()}
   */
  @Nullable Object invoke(Method method, @Nullable Object[] arguments, Invocation invocation) throws Throwable;

}
