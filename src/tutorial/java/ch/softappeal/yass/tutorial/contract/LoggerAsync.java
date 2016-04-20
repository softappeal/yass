package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.InterceptorAsync;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public class LoggerAsync implements InterceptorAsync<Integer> {

    private final AtomicInteger id = new AtomicInteger(0);

    @Override public Integer entry(final MethodMapper.Mapping methodMapping, final @Nullable Object[] arguments) {
        final Integer context = id.getAndIncrement();
        System.out.println("entry " + context + ": " + methodMapping.method.getName() + " " + Logger.dump(arguments));
        return context;
    }

    @Override public void exit(final Integer context, final @Nullable Object result) {
        System.out.println("exit " + context + ": " + Logger.dump(result));
    }

    @Override public void exception(final Integer context, final Exception exception) {
        System.out.println("exception " + context + ": " + exception);
    }

}
