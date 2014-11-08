package ch.softappeal.yass.tutorial.server;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.util.Check;
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

    @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
        System.out.println(new Date() + " - " + Session.get().hashCode() + " - " + side + ": " + method.getName());
        return invocation.proceed();
    }

    public static final Interceptor CLIENT = new Logger("client");
    public static final Interceptor SERVER = new Logger("server");

}
