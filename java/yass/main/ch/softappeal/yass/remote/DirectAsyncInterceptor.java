package ch.softappeal.yass.remote;

import ch.softappeal.yass.Nullable;

public final class DirectAsyncInterceptor implements AsyncInterceptor {

    private DirectAsyncInterceptor() {
        // disable
    }

    @Override public void entry(final AbstractInvocation invocation) {
        // empty
    }

    @Override public void exit(final AbstractInvocation invocation, @Nullable final Object result) {
        // empty
    }

    @Override public void exception(final AbstractInvocation invocation, final Exception exception) {
        // empty
    }

    public static final AsyncInterceptor INSTANCE = new DirectAsyncInterceptor();

}
