package ch.softappeal.yass.util.test;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public final class RejectExecutor implements Executor {

    public static final RejectExecutor INSTANCE = new RejectExecutor();

    private RejectExecutor() {
        // disable
    }

    @Override public void execute(final Runnable command) {
        throw new RejectedExecutionException();
    }

}
