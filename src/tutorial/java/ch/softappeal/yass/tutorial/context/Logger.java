package ch.softappeal.yass.tutorial.context;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.util.Check;

import java.util.Arrays;

/**
 * Shows how to implement an {@link Interceptor}.
 */
public class Logger implements Interceptor {

  public Logger(final String name) {
    this.name = Check.notNull(name);
  }

  private final String name;

  private void log(final Object message, final Invocation invocation) {
    System.out.println(name + " | " + message + " | " + invocation.context);
  }

  @Override public Object invoke(final Invocation invocation) throws Throwable {
    log(invocation.method + " | " + Arrays.deepToString(invocation.arguments), invocation);
    try {
      final Object result = invocation.proceed();
      log(result, invocation);
      return result;
    } catch (final Throwable t) {
      log(t, invocation);
      throw t;
    }
  }

}
