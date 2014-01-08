package ch.softappeal.yass.core.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class InterceptorsTest {

  public static final Method METHOD;

  static {
    try {
      METHOD = Object.class.getMethod("toString");
    } catch (final NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  private static final Object[] ARGUMENTS = new Object[0];

  @Test public void direct() throws Throwable {
    final Object result = new Object();
    Assert.assertSame(
      Interceptors.DIRECT.invoke(null, null, new Invocation() {
        @Override public Object proceed() {
          return result;
        }
      }),
      result
    );
  }

  private int step;

  private final class StepInterceptor implements Interceptor {

    private final int begin;
    private final int end;

    StepInterceptor(final int begin, final int end) {
      this.begin = begin;
      this.end = end;
    }

    @Override @Nullable public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
      System.out.println("enter: begin=" + begin + " step=" + step);
      Assert.assertSame(METHOD, method);
      Assert.assertSame(ARGUMENTS, arguments);
      Assert.assertEquals(begin, step);
      step++;
      final Object result = invocation.proceed();
      System.out.println("exit : end  =" + end + " step=" + step + " result=" + result);
      Assert.assertSame(METHOD, method);
      Assert.assertSame(ARGUMENTS, arguments);
      Assert.assertEquals(end, step);
      Assert.assertEquals(result, step + 100);
      step++;
      return step + 100;
    }

  }

  private void test(final Interceptor interceptor, final int interceptors) throws Throwable {
    step = 0;
    final Invocation invocation = new Invocation() {
      @Override public Object proceed() {
        Assert.assertEquals(step, interceptors);
        step++;
        return step + 100;
      }
    };
    Assert.assertEquals(interceptor.invoke(METHOD, ARGUMENTS, invocation), (2 * interceptors) + 101);
    Assert.assertEquals(step, (2 * interceptors) + 1);
  }

  @Test public void composite() throws Throwable {
    final Interceptor stepInterceptor = new StepInterceptor(0, 0);
    Assert.assertSame(stepInterceptor, Interceptors.composite(stepInterceptor, Interceptors.DIRECT));
    Assert.assertSame(stepInterceptor, Interceptors.composite(Interceptors.DIRECT, stepInterceptor));
    test(
      Interceptors.composite(
        new StepInterceptor(0, 8),
        new StepInterceptor(1, 7),
        new StepInterceptor(2, 6),
        new StepInterceptor(3, 5)
      ),
      4
    );
  }

  @Test public void threadLocal() throws Throwable {
    final ThreadLocal<String> threadLocal = new ThreadLocal<>();
    try {
      Interceptors.getInvocation(threadLocal);
      Assert.fail();
    } catch (final IllegalStateException e) {
      Assert.assertEquals("no active invocation", e.getMessage());
    }
    Assert.assertFalse(Interceptors.hasInvocation(threadLocal));
    final String oldValue = "oldValue";
    threadLocal.set(oldValue);
    final String value = "value";
    final Object result = new Object();
    Assert.assertSame(
      result,
      Interceptors.threadLocal(threadLocal, value).invoke(null, null, new Invocation() {
        @Override public Object proceed() {
          Assert.assertTrue(Interceptors.hasInvocation(threadLocal));
          Assert.assertSame(value, Interceptors.getInvocation(threadLocal));
          return result;
        }
      })
    );
    Assert.assertSame(oldValue, threadLocal.get());
  }

}
