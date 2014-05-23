package ch.softappeal.yass.core.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class InterceptorTest {

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
      Interceptor.DIRECT.invoke(null, null, () -> result),
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
      Assert.assertEquals(end, step);
      Assert.assertEquals(result, step + 100);
      step++;
      return step + 100;
    }
  }

  @Test public void composite() throws Throwable {
    final Interceptor stepInterceptor = new StepInterceptor(0, 0);
    Assert.assertSame(stepInterceptor, Interceptor.composite(stepInterceptor, Interceptor.DIRECT));
    Assert.assertSame(stepInterceptor, Interceptor.composite(Interceptor.DIRECT, stepInterceptor));
    final Interceptor interceptor = Interceptor.composite(
      new StepInterceptor(0, 8),
      new StepInterceptor(1, 7),
      new StepInterceptor(2, 6),
      new StepInterceptor(3, 5)
    );
    final int interceptors = 4;
    step = 0;
    final Invocation invocation = () -> {
      Assert.assertEquals(step, interceptors);
      step++;
      return step + 100;
    };
    Assert.assertEquals(interceptor.invoke(METHOD, ARGUMENTS, invocation), (2 * interceptors) + 101);
    Assert.assertEquals(step, (2 * interceptors) + 1);
  }

  @Test public void threadLocal() throws Throwable {
    final ThreadLocal<String> threadLocal = new ThreadLocal<>();
    final String oldValue = "oldValue";
    threadLocal.set(oldValue);
    final String value = "value";
    final Object result = new Object();
    Assert.assertSame(
      result,
      Interceptor.threadLocal(threadLocal, value).invoke(null, null, () -> {
        Assert.assertSame(value, threadLocal.get());
        return result;
      })
    );
    Assert.assertSame(oldValue, threadLocal.get());
  }

}
