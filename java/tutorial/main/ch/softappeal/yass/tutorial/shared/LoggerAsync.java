package ch.softappeal.yass.tutorial.shared;

import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.remote.AbstractInvocation;
import ch.softappeal.yass.remote.InterceptorAsync;

public class LoggerAsync implements InterceptorAsync {

    private LoggerAsync() {
        // disable
    }

    @Override public void entry(final AbstractInvocation invocation) throws Exception {
        System.out.println("entry " + invocation.hashCode() + ": " + invocation.methodMapping.method.getName() + " " + Logger.dump(invocation.arguments));
    }
    @Override public void exit(final AbstractInvocation invocation, @Nullable final Object result) throws Exception {
        System.out.println("exit " + invocation.hashCode() + ": " + invocation.methodMapping.method.getName() + " " + Logger.dump(result));
    }
    @Override public void exception(final AbstractInvocation invocation, final Exception exception) throws Exception {
        System.out.println("exception " + invocation.hashCode() + ": " + invocation.methodMapping.method.getName() + " " + exception);
    }

    public static final InterceptorAsync INSTANCE = new LoggerAsync();

}
