package ch.softappeal.yass.remote;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleInterceptorContext {

    private static final AtomicInteger ID = new AtomicInteger(0);

    public final int id = ID.getAndIncrement();
    public final MethodMapper.Mapping methodMapping;
    public final List<Object> arguments;

    public SimpleInterceptorContext(final MethodMapper.Mapping methodMapping, final List<Object> arguments) {
        this.methodMapping = Objects.requireNonNull(methodMapping);
        this.arguments = Objects.requireNonNull(arguments);
    }

}
