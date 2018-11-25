package ch.softappeal.yass.tutorial.shared;

import ch.softappeal.yass.remote.AbstractInvocation;
import ch.softappeal.yass.remote.AsyncInterceptor;

import static ch.softappeal.yass.DumperKt.dump;

public class AsyncLogger implements AsyncInterceptor {

    private AsyncLogger() {
        // disable
    }

    @Override
    public void entry(final AbstractInvocation invocation) throws Exception {
        System.out.println(
            "entry " + invocation.hashCode() + ": " + invocation.getMethodMapping().getMethod().getName() + " " +
                dump(Logger.DUMPER, invocation.getArguments())
        );
    }

    @Override
    public void exit(final AbstractInvocation invocation, final Object result) throws Exception {
        System.out.println(
            "exit " + invocation.hashCode() + ": " + invocation.getMethodMapping().getMethod().getName() + " " +
                dump(Logger.DUMPER, result)
        );
    }

    @Override
    public void exception(final AbstractInvocation invocation, final Exception exception) throws Exception {
        System.out.println(
            "exception " + invocation.hashCode() + ": " + invocation.getMethodMapping().getMethod().getName() + " " +
                exception
        );
    }

    public static final AsyncInterceptor INSTANCE = new AsyncLogger();

}
