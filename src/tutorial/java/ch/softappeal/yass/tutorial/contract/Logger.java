package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Dumper;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * Shows how to implement an {@link Interceptor}.
 */
public final class Logger implements Interceptor {

    public enum Side {CLIENT, SERVER}

    private final @Nullable Session session;
    private final Side side;

    public Logger(final @Nullable Session session, final Side side) {
        this.session = session;
        this.side = Check.notNull(side);
    }

    private static final Dumper DUMPER = new Dumper(true, true);
    private static String dump(final Object value) {
        return DUMPER.append(new StringBuilder(256), value).toString();
    }

    private void log(final String type, final Method method, final Object data) {
        System.out.printf(
            "%tT | %s | %s | %s | %s | %s\n",
            new Date(),
            (session == null) ? "<no-session>" : session,
            side,
            type,
            method.getName(),
            dump(data)
        );
    }

    @Override public @Nullable Object invoke(final Method method, final @Nullable Object[] arguments, final Invocation invocation) throws Exception {
        final boolean oneWay = method.isAnnotationPresent(OneWay.class);
        log(oneWay ? "oneWay" : "entry", method, arguments);
        try {
            final Object result = invocation.proceed();
            if (!oneWay) {
                log("exit", method, result);
            }
            return result;
        } catch (final Exception e) {
            log("exception", method, e);
            throw e;
        }
    }

}
