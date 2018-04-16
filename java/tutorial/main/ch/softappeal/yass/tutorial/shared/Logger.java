package ch.softappeal.yass.tutorial.shared;

import ch.softappeal.yass.Dumper;
import ch.softappeal.yass.Interceptor;
import ch.softappeal.yass.Invocation;
import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.remote.session.Session;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Objects;

/**
 * Shows how to implement an {@link Interceptor}.
 */
public final class Logger implements Interceptor {

    public enum Side {CLIENT, SERVER}

    private final @Nullable Session session;
    private final Side side;

    public Logger(final @Nullable Session session, final Side side) {
        this.session = session;
        this.side = Objects.requireNonNull(side);
    }

    private static final Dumper DUMPER = new Dumper(true, true);
    public static String dump(final @Nullable Object value) {
        return DUMPER.dump(value);
    }

    private void log(final String type, final Method method, final Object data) {
        System.out.printf(
            "%tT | %s | %s | %s | %s | %s%n",
            new Date(), (session == null) ? "<no-session>" : session, side, type, method.getName(), dump(data)
        );
    }

    @Override public @Nullable Object invoke(final Method method, final @Nullable Object[] arguments, final Invocation invocation) throws Exception {
        log("entry", method, arguments);
        try {
            final var result = invocation.proceed();
            log("exit", method, result);
            return result;
        } catch (final Exception e) {
            log("exception", method, e);
            throw e;
        }
    }

}
