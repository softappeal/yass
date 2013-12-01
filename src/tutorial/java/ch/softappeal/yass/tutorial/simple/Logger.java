package ch.softappeal.yass.tutorial.simple;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;

public final class Logger implements Interceptor {

  @Override public Object invoke(final Invocation invocation) throws Throwable {
    System.out.println("logger: " + invocation.method);
    return invocation.proceed();
  }

}
