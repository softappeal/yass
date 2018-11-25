package ch.softappeal.yass;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static ch.softappeal.yass.InterceptorKt.proxy;
import static ch.softappeal.yass.InterceptorTestKt.getJavaCalculatorImpl;
import static ch.softappeal.yass.InterceptorTestKt.getMethod;

public class JInterceptorTest {
    @Test
    public void test() {
        final Function3<Method, List<?>, Function0<?>, Object> interceptor = new TestInterceptor();
        final List<?> arguments = Collections.emptyList();
        final RuntimeException exception = new RuntimeException();
        final Object result = "result";
        Assertions.assertSame(result, interceptor.invoke(getMethod(), arguments, () -> result));
        try {
            interceptor.invoke(getMethod(), Collections.emptyList(), () -> {
                throw exception;
            });
            Assertions.fail("");
        } catch (final Exception e) {
            Assertions.assertSame(exception, e);
        }
    }

    @Test
    public void proxyTest() {
        final JavaCalculator calculator = proxy(JavaCalculator.class, getJavaCalculatorImpl());
        Assertions.assertEquals(2, calculator.divide(6, 3));
        try {
            Assertions.assertEquals(2, calculator.divide(6, 0));
            Assertions.fail("");
        } catch (final ArithmeticException e) {
            System.out.println(e.getMessage());
        }
        Assertions.assertEquals(1, calculator.one());
        Assertions.assertEquals(-2, calculator.minus(2));
        Assertions.assertEquals("echo", calculator.echo("echo"));
        Assertions.assertNull(calculator.echo(null));
    }
}
