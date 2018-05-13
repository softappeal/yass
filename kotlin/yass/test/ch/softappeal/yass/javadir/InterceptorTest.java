package ch.softappeal.yass.javadir;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class InterceptorTest {

    private static final Method METHOD;
    static {
        try {
            METHOD = Object.class.getMethod("toString");
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test() throws Exception {
        final Function3<Method, List<?>, Function0<?>, Object> interceptor = new TestInterceptor();
        final List<?> arguments = Collections.emptyList();
        final RuntimeException exception = new RuntimeException();
        final Object result = "result";
        assertSame(result, interceptor.invoke(METHOD, arguments, () -> result));
        try {
            interceptor.invoke(METHOD, Collections.emptyList(), () -> {
                throw exception;
            });
            fail();
        } catch (final Exception e) {
            assertSame(exception, e);
        }
    }

}
