package ch.softappeal.yass.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;

public final class Exceptions {

    private Exceptions() {
        // disable
    }

    public static RuntimeException wrap(final Exception e) {
        return (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(Check.notNull(e));
    }

    public static void uncaughtException(final UncaughtExceptionHandler uncaughtExceptionHandler, final Throwable throwable) {
        uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), throwable);
    }

    /**
     * Prints to {@link System#err} and EXITS if {@code !(throwable instanceof Exception)}.
     */
    public static final UncaughtExceptionHandler STD_ERR = (thread, throwable) -> {
        try {
            System.err.println("### UncaughtExceptionHandler of '" + Exceptions.class.getName() + "' caught exception at '" + new Date() + "' in thread '" + ((thread == null) ? "<null>" : thread.getName()) + "':");
            if (throwable == null) {
                System.err.println("throwable is null");
            } else {
                throwable.printStackTrace();
            }
        } finally {
            if (!(throwable instanceof Exception)) {
                System.exit(1);
            }
        }
    };

    /**
     * Prints to {@link System#err} and EXITS.
     */
    public static final UncaughtExceptionHandler TERMINATE = (thread, throwable) -> {
        try {
            STD_ERR.uncaughtException(thread, throwable);
        } finally {
            System.exit(1);
        }
    };

}
