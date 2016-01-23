package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public abstract class ProxyDelegate<S extends Session> {

    private volatile @Nullable S session = null;

    protected final void session(final @Nullable S session) {
        this.session = session;
    }

    private static boolean connected(final Session session) {
        return (session != null) && !session.isClosed();
    }

    public final boolean connected() {
        return connected(session);
    }

    /**
     * @return current {@link Session}
     * @throws SessionClosedException if no active session
     */
    public final S session() throws SessionClosedException {
        final S session = this.session;
        if (!connected(session)) {
            throw new SessionClosedException();
        }
        return session;
    }

    public interface SessionProxyGetter<S, C> {
        C get(S session) throws Exception;
    }

    /**
     * @return a proxy delegating to {@link #session()}
     */
    protected final <C> C proxy(final Class<C> contract, final SessionProxyGetter<S, C> sessionProxyGetter) {
        Check.notNull(sessionProxyGetter);
        return contract.cast(Proxy.newProxyInstance(
            contract.getClassLoader(),
            new Class<?>[] {contract},
            new InvocationHandler() {
                @Override public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    final C impl = sessionProxyGetter.get(ProxyDelegate.this.session());
                    try {
                        return method.invoke(impl, arguments);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            }
        ));
    }

}
