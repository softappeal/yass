package ch.softappeal.yass;

import kotlin.jvm.functions.*;
import org.jetbrains.annotations.*;

import java.lang.reflect.*;
import java.util.*;

public class TestInterceptor implements Function3<Method, List<?>, Function0<?>, Object> {
    @Override
    public Object invoke(
        @NotNull final Method method, @NotNull final List<?> arguments, @NotNull final Function0<?> invocation
    ) {
        return invocation.invoke();
    }
}
