package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

import java.util.List;
import java.util.Objects;

public abstract class AbstractInvocation {

    public final MethodMapper.Mapping methodMapping;
    public final List<Object> arguments;
    private final @Nullable InterceptorAsync<Object> interceptor;
    private volatile @Nullable Object context;

    AbstractInvocation(final MethodMapper.Mapping methodMapping, final List<Object> arguments, final @Nullable InterceptorAsync<Object> interceptor) {
        this.methodMapping = Objects.requireNonNull(methodMapping);
        this.arguments = Objects.requireNonNull(arguments);
        this.interceptor = interceptor;
    }

    final boolean async() {
        return interceptor != null;
    }

    final void entry() throws Exception {
        context = interceptor.entry(methodMapping, arguments);
    }

    final @Nullable Object exit(final @Nullable Object result) throws Exception {
        return interceptor.exit(context, result);
    }

    final Exception exception(final Exception exception) throws Exception {
        return interceptor.exception(context, exception);
    }

}
