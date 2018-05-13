package ch.softappeal.yass.javadir;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;

public class TestInterceptor implements Function3<Method, List<?>, Function0<?>, Object> {

    @Override public Object invoke(
        @NotNull final Method method, @NotNull final List<?> arguments, @NotNull final Function0<?> invocation
    ) {
        return invocation.invoke();
    }

}
