package ch.softappeal.yass;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class NamedThreadFactory implements ThreadFactory {

    private final String name;
    private final UncaughtExceptionHandler uncaughtExceptionHandler;
    private final int priority;
    private final boolean daemon;
    private final AtomicInteger number = new AtomicInteger(1);

    public NamedThreadFactory(final String name, final UncaughtExceptionHandler uncaughtExceptionHandler, final int priority, final boolean daemon) {
        this.name = Objects.requireNonNull(name);
        this.uncaughtExceptionHandler = Objects.requireNonNull(uncaughtExceptionHandler);
        this.priority = priority;
        this.daemon = daemon;
    }

    public NamedThreadFactory(final String name, final UncaughtExceptionHandler uncaughtExceptionHandler, final int priority) {
        this(name, uncaughtExceptionHandler, priority, false);
    }

    public NamedThreadFactory(final String name, final UncaughtExceptionHandler uncaughtExceptionHandler) {
        this(name, uncaughtExceptionHandler, Thread.NORM_PRIORITY);
    }

    @Override public Thread newThread(final Runnable r) {
        final var thread = new Thread(r, name + '-' + number.getAndIncrement());
        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        if (thread.getPriority() != priority) {
            thread.setPriority(priority);
        }
        if (thread.isDaemon() != daemon) {
            thread.setDaemon(daemon);
        }
        return thread;
    }

}
