package ch.softappeal.yass;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static ch.softappeal.yass.InterceptorKt.proxy;
import static ch.softappeal.yass.InterceptorTestKt.getJavaCalculatorImpl;
import static ch.softappeal.yass.InterceptorTestKt.getMethod;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class JInterceptorTest {
    @Test
    public void test() {
        final Function3<Method, List<?>, Function0<?>, Object> interceptor = new TestInterceptor();
        final List<?> arguments = Collections.emptyList();
        final RuntimeException exception = new RuntimeException();
        final Object result = "result";
        assertSame(result, interceptor.invoke(getMethod(), arguments, () -> result));
        try {
            interceptor.invoke(getMethod(), Collections.emptyList(), () -> {
                throw exception;
            });
            fail();
        } catch (final Exception e) {
            assertSame(exception, e);
        }
    }

    @Test
    public void proxyTest() {
        final JavaCalculator calculator = proxy(JavaCalculator.class, getJavaCalculatorImpl());
        Assert.assertEquals(2, calculator.divide(6, 3));
        try {
            Assert.assertEquals(2, calculator.divide(6, 0));
            fail();
        } catch (final ArithmeticException e) {
            System.out.println(e.getMessage());
        }
        Assert.assertEquals(1, calculator.one());
        Assert.assertEquals(-2, calculator.minus(2));
        Assert.assertEquals("echo", calculator.echo("echo"));
        Assert.assertNull(calculator.echo(null));
    }
}
