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

    private final String side;

    private Logger(final String side) {
        this.side = Check.notNull(side);
    }

    private static final Dumper DUMPER = new Dumper(true, false);
    private static String dump(final Object value) {
        return DUMPER.append(new StringBuilder(256), value).toString();
    }

    private void log(final String type, final Method method, final Object data) {
        final Session session = Session.get();
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

    @Override public Object invoke(final Method method, final @Nullable Object[] arguments, final Invocation invocation) throws Throwable {
        final boolean oneWay = method.isAnnotationPresent(OneWay.class);
        log(oneWay ? "oneWay" : "entry", method, arguments);
        try {
            final Object result = invocation.proceed();
            if (!oneWay) {
                log("exit", method, result);
            }
            return result;
        } catch (final Throwable throwable) {
            log("exception", method, throwable);
            throw throwable;
        }
    }

    public static final Interceptor CLIENT = new Logger("client");
    public static final Interceptor SERVER = new Logger("server");

}
