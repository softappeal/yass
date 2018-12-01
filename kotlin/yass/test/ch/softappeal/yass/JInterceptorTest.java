package ch.softappeal.yass;

import kotlin.jvm.functions.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.util.*;

import static ch.softappeal.yass.InterceptorKt.*;
import static ch.softappeal.yass.InterceptorTestKt.*;

class JInterceptorTest {
    @Test
    void test() {
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
    void proxyTest() {
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
