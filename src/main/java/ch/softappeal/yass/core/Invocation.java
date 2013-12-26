package ch.softappeal.yass.core;

import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Method;

/**
 * Represents a method invocation.
 * @see Interceptor
 */
public abstract class Invocation {

  public final Method method;
  @Nullable public final Object[] arguments;

  protected Invocation(final Method method, @Nullable final Object[] arguments) {
    this.method = method;
    //noinspection AssignmentToCollectionOrArrayFieldFromParameter
    this.arguments = arguments;
  }

  /**
   * Proceeds with the invocation.
   * @return the result of the invocation
   * @throws Throwable the exception of the invocation
   */
  @Nullable public abstract Object proceed() throws Throwable;

}
