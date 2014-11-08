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
     * <p>
     * $note: For productive code, this handler should be replaced with one that uses your logging framework!
     */
    public static final UncaughtExceptionHandler STD_ERR = (thread, throwable) -> {
        System.err.println(
            "### " + new Date() + " - " + ((thread == null) ? "<null>" : thread.getName()) + " - " + Exceptions.class.getName() + ':'
        );
        if (throwable == null) {
            System.err.println("throwable is null");
        } else {
            throwable.printStackTrace();
            if (!(throwable instanceof Exception)) {
                System.exit(1);
            }
        }
    };

    /**
     * Prints to {@link System#err} and EXITS.
     * <p>
     * $note: For productive code, this handler should be replaced with one that uses your logging framework!
     */
    public static final UncaughtExceptionHandler TERMINATE = (thread, throwable) -> {
        STD_ERR.uncaughtException(thread, throwable);
        System.exit(1);
    };

}
