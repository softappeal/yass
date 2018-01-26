package ch.softappeal.yass.tutorial.shared;

import ch.softappeal.yass.core.remote.InterceptorAsync;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.SimpleInterceptorContext;
import ch.softappeal.yass.util.Nullable;

import java.util.List;

public class LoggerAsync implements InterceptorAsync<SimpleInterceptorContext> {

    private LoggerAsync() {
        // disable
    }

    @Override public SimpleInterceptorContext entry(final MethodMapper.Mapping methodMapping, final List<Object> arguments) {
        final SimpleInterceptorContext context = new SimpleInterceptorContext(methodMapping, arguments);
        System.out.println("entry " + context.id + ": " + methodMapping.method.getName() + " " + Logger.dump(arguments));
        return context;
    }

    @Override public @Nullable Object exit(final SimpleInterceptorContext context, final @Nullable Object result) {
        System.out.println("exit " + context.id + ": " + context.methodMapping.method.getName() + " " + Logger.dump(result));
        return result;
    }

    @Override public Exception exception(final SimpleInterceptorContext context, final Exception exception) {
        System.out.println("exception " + context.id + ": " + context.methodMapping.method.getName() + " " + exception);
        return exception;
    }

    public static final InterceptorAsync<?> INSTANCE = new LoggerAsync();

}
