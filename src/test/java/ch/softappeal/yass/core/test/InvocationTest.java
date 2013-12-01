package ch.softappeal.yass.core.test;

import ch.softappeal.yass.core.Invocation;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class InvocationTest {

  public static final Method METHOD;

  static {
    try {
      METHOD = Object.class.getMethod("toString");
    } catch (final NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  static final Object[] ARGUMENTS = new Object[0];

  @Test public void protectedConstructor() {
    final Invocation invocation = new Invocation(METHOD, ARGUMENTS) {
      @Override public Object proceed() throws Throwable {
        return null;
      }
    };
    Assert.assertSame(METHOD, invocation.method);
    Assert.assertSame(ARGUMENTS, invocation.arguments);
    Assert.assertNull(invocation.context);
  }

}
