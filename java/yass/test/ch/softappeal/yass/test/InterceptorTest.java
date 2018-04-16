package ch.softappeal.yass.test;

import ch.softappeal.yass.Interceptor;
import ch.softappeal.yass.Invocation;
import ch.softappeal.yass.Nullable;
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

    @Test public void direct() throws Exception {
        final var result = new Object();
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
        @Override public @Nullable Object invoke(final Method method, final @Nullable Object[] arguments, final Invocation invocation) throws Exception {
            System.out.println("enter: begin=" + begin + " step=" + step);
            Assert.assertSame(METHOD, method);
            Assert.assertSame(ARGUMENTS, arguments);
            Assert.assertEquals(begin, step);
            step++;
            final var result = invocation.proceed();
            System.out.println("exit : end  =" + end + " step=" + step + " result=" + result);
            Assert.assertEquals(end, step);
            Assert.assertEquals(result, step + 100);
            step++;
            return step + 100;
        }
    }

    @Test public void composite() throws Exception {
        final Interceptor stepInterceptor = new StepInterceptor(0, 0);
        Assert.assertSame(stepInterceptor, Interceptor.composite(stepInterceptor, Interceptor.DIRECT));
        Assert.assertSame(stepInterceptor, Interceptor.composite(Interceptor.DIRECT, stepInterceptor));
        final var interceptor = Interceptor.composite(
            new StepInterceptor(0, 8),
            new StepInterceptor(1, 7),
            new StepInterceptor(2, 6),
            new StepInterceptor(3, 5)
        );
        final var interceptors = 4;
        step = 0;
        final Invocation invocation = () -> {
            Assert.assertEquals(step, interceptors);
            step++;
            return step + 100;
        };
        Assert.assertEquals(interceptor.invoke(METHOD, ARGUMENTS, invocation), (2 * interceptors) + 101);
        Assert.assertEquals(step, (2 * interceptors) + 1);
    }

    @Test public void threadLocal() throws Exception {
        final ThreadLocal<String> threadLocal = new ThreadLocal<>();
        final var oldValue = "oldValue";
        threadLocal.set(oldValue);
        final var value = "value";
        final var result = new Object();
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
