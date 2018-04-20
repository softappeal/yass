package ch.softappeal.yass.remote.session;

import ch.softappeal.yass.Interceptor;
import ch.softappeal.yass.Nullable;

import java.util.Objects;
import java.util.function.Function;

public abstract class ProxyDelegate<S extends Session> {

    private volatile @Nullable S session = null;

    protected final void session(final @Nullable S session) {
        this.session = session;
    }

    private static boolean connected(final @Nullable Session session) {
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
        final var session = this.session;
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
        return Interceptor.proxy(contract, (proxy, method, arguments) -> Interceptor.invoke(method, proxyGetter.apply(session()), arguments));
    }

}
