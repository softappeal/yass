package ch.softappeal.yass.core;

import ch.softappeal.yass.util.Nullable;

/**
 * Intercepts a method invocation.
 * @see Interceptors
 */
public interface Interceptor {

  /**
   * Performs extra treatments before and after the invocation.
   * @return {@link Invocation#proceed()}
   * @throws Throwable exception of {@link Invocation#proceed()}
   */
  @Nullable Object invoke(Invocation invocation) throws Throwable;

}
