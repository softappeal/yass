package ch.softappeal.yass.core;

import ch.softappeal.yass.util.ContextLocator;

/**
 * Links {@link Invocation#context} with {@link ContextLocator#context()}.
 */
public final class ContextInterceptor<C> implements ContextLocator<C>, Interceptor {


  private ContextInterceptor() {
    // disable
  }

  private static final ContextInterceptor<Object> INSTANCE = new ContextInterceptor<>();

  @SuppressWarnings("unchecked")
  public static <C> ContextInterceptor<C> get() {
    return (ContextInterceptor<C>)INSTANCE;
  }



  private static final ThreadLocal<Object> THREAD_LOCAL = new ThreadLocal<>();


  @SuppressWarnings("unchecked")
  @Override public C context() {
    return (C)Interceptors.getInvocation(THREAD_LOCAL);
  }


  @Override public Object invoke(final Invocation invocation) throws Throwable {
    return Interceptors.threadLocal(THREAD_LOCAL, invocation.context).invoke(invocation);
  }


}
