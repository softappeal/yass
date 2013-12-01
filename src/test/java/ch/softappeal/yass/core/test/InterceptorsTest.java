package ch.softappeal.yass.core.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

public class InterceptorsTest {

  @Test public void direct() throws Throwable {
    final Object result = new Object();
    Assert.assertSame(
      Interceptors.DIRECT.invoke(new Invocation(InvocationTest.METHOD, InvocationTest.ARGUMENTS) {
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

    @Override @Nullable public Object invoke(final Invocation invocation) throws Throwable {
      System.out.println("enter: begin=" + begin + " step=" + step + " context=" + invocation.context);
      Assert.assertSame(InvocationTest.METHOD, invocation.method);
      Assert.assertSame(InvocationTest.ARGUMENTS, invocation.arguments);
      Assert.assertEquals(begin, step);
      Assert.assertEquals(begin + 100, invocation.context);
      step++;
      invocation.context = step + 100;
      final Object result = invocation.proceed();
      System.out.println("exit : end  =" + end + " step=" + step + " context=" + invocation.context + " result=" + result);
      Assert.assertSame(InvocationTest.METHOD, invocation.method);
      Assert.assertSame(InvocationTest.ARGUMENTS, invocation.arguments);
      Assert.assertEquals(end, step);
      Assert.assertEquals(end + 100, invocation.context);
      Assert.assertEquals(result, step + 200);
      step++;
      invocation.context = step + 100;
      return step + 200;
    }

  }

  private void test(final Interceptor interceptor, final int interceptors) throws Throwable {
    step = 0;
    final Invocation invocation = new Invocation(InvocationTest.METHOD, InvocationTest.ARGUMENTS) {
      {
        Assert.assertSame(InvocationTest.METHOD, method);
        Assert.assertSame(InvocationTest.ARGUMENTS, arguments);
        context = 100;
      }
      @Override public Object proceed() {
        Assert.assertSame(InvocationTest.METHOD, method);
        Assert.assertSame(InvocationTest.ARGUMENTS, arguments);
        Assert.assertEquals(step, interceptors);
        Assert.assertEquals(interceptors + 100, context);
        step++;
        context = step + 100;
        return step + 200;
      }
    };
    Assert.assertEquals(interceptor.invoke(invocation), (2 * interceptors) + 201);
    Assert.assertSame(InvocationTest.METHOD, invocation.method);
    Assert.assertSame(InvocationTest.ARGUMENTS, invocation.arguments);
    Assert.assertEquals(step, (2 * interceptors) + 1);
    Assert.assertEquals(invocation.context, (2 * interceptors) + 101);
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
      Interceptors.threadLocal(threadLocal, value).invoke(new Invocation(InvocationTest.METHOD, InvocationTest.ARGUMENTS) {
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
