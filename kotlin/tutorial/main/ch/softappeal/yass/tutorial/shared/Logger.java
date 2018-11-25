package ch.softappeal.yass.tutorial.shared;

import ch.softappeal.yass.remote.session.Session;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static ch.softappeal.yass.DumperKt.dump;
import static ch.softappeal.yass.DumperKt.graphDumper;

/**
 * Shows how to implement an interceptor.
 */
public final class Logger implements Function3<Method, List<?>, Function0<?>, Object> {

    public enum Side {CLIENT, SERVER}

    private final Session session;
    private final Side side;

    public Logger(final Session session, final Side side) {
        this.session = session;
        this.side = Objects.requireNonNull(side);
    }

    public static final Function2<StringBuilder, Object, StringBuilder> DUMPER = graphDumper(true);

    private void log(final String type, final Method method, final Object data) {
        System.out.printf(
            "%tT | %s | %s | %s | %s | %s%n",
            new Date(), (session == null) ? "<no-session>" : session, side, type, method.getName(), dump(DUMPER, data)
        );
    }

    @Override
    public Object invoke(final Method method, final List<?> arguments, final Function0<?> invocation) {
        log("entry", method, arguments);
        try {
            final Object result = invocation.invoke();
            log("exit", method, result);
            return result;
        } catch (final Exception e) {
            log("exception", method, e);
            throw e;
        }
    }

}
