package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.InterceptorAsync;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.SimpleInterceptorContext;
import ch.softappeal.yass.util.Nullable;

public class LoggerAsync implements InterceptorAsync<SimpleInterceptorContext> {

    @Override public SimpleInterceptorContext entry(final MethodMapper.Mapping methodMapping, final @Nullable Object[] arguments) {
        final SimpleInterceptorContext context = new SimpleInterceptorContext(methodMapping, arguments);
        System.out.println("entry " + context.id + ": " + methodMapping.method.getName() + " " + Logger.dump(arguments));
        return context;
    }

    @Override public void exit(final SimpleInterceptorContext context, final @Nullable Object result) {
        System.out.println("exit " + context.id + ": " + context.methodMapping.method.getName() + " " + Logger.dump(result));
    }

    @Override public void exception(final SimpleInterceptorContext context, final Exception exception) {
        System.out.println("exception " + context.id + ": " + context.methodMapping.method.getName() + " " + exception);
    }

}
