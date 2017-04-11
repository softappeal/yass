package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.function.Function;

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

    /**
     * @return a proxy delegating to {@link #session()}
     */
    public final <C> C proxy(final Class<C> contract, final Function<S, C> proxyGetter) {
        Objects.requireNonNull(proxyGetter);
        return contract.cast(Proxy.newProxyInstance(
            contract.getClassLoader(),
            new Class<?>[] {contract},
            (proxy, method, arguments) -> {
                final C impl = proxyGetter.apply(session());
                try {
                    return method.invoke(impl, arguments);
                } catch (final InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        ));
    }

}
