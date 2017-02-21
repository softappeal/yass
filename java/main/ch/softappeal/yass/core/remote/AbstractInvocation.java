package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.util.List;

public abstract class AbstractInvocation {

    public final MethodMapper.Mapping methodMapping;
    public final List<Object> arguments;
    private final @Nullable InterceptorAsync<Object> interceptor;
    private volatile Object context;

    AbstractInvocation(final MethodMapper.Mapping methodMapping, final List<Object> arguments, final @Nullable InterceptorAsync<Object> interceptor) {
        this.methodMapping = Check.notNull(methodMapping);
        this.arguments = Check.notNull(arguments);
        this.interceptor = interceptor;
    }

    final boolean async() {
        return interceptor != null;
    }

    final void entry() throws Exception {
        context = interceptor.entry(methodMapping, arguments);
    }

    final void exit(@Nullable Object result) throws Exception {
        interceptor.exit(context, result);
    }

    final void exception(Exception exception) throws Exception {
        interceptor.exception(context, exception);
    }

}
