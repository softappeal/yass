package ch.softappeal.yass.remote;

import ch.softappeal.yass.Nullable;

public interface AsyncInterceptor {

    void entry(AbstractInvocation invocation) throws Exception;

    void exit(AbstractInvocation invocation, @Nullable Object result) throws Exception;

    void exception(AbstractInvocation invocation, Exception exception) throws Exception;

}
