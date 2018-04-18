package ch.softappeal.yass.remote;

import ch.softappeal.yass.Nullable;

import java.util.List;
import java.util.Objects;

public abstract class AbstractInvocation {

    public final MethodMapper.Mapping methodMapping;
    public final List<Object> arguments;
    private final @Nullable AsyncInterceptor interceptor;
    public volatile @Nullable Object context;

    AbstractInvocation(final MethodMapper.Mapping methodMapping, final List<Object> arguments, final @Nullable AsyncInterceptor interceptor) {
        this.methodMapping = Objects.requireNonNull(methodMapping);
        this.arguments = Objects.requireNonNull(arguments);
        this.interceptor = interceptor;
    }

    final boolean async() {
        return interceptor != null;
    }

    final void entry() throws Exception {
        interceptor.entry(this);
    }

    final void exit(final @Nullable Object result) throws Exception {
        interceptor.exit(this, result);
    }

    final void exception(final Exception exception) throws Exception {
        interceptor.exception(this, exception);
    }

}
