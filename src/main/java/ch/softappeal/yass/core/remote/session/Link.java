package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

/**
 * Wraps a client side {@link Session}.
 * {@link Delegate#proxy} survives {@link Session#closed(Throwable)}.
 * @see Reconnector
 */
public abstract class Link {

    @Nullable private Delegate<Object> firstDelegate = null;

    private void forEachDelegate(final Consumer<Delegate<Object>> consumer) {
        for (Delegate<Object> delegate = firstDelegate; delegate != null; delegate = delegate.nextDelegate) {
            consumer.accept(delegate);
        }
    }

    protected final <C> Delegate<C> delegate(final ContractId<C> contractId, final Interceptor... interceptors) {
        return new Delegate<>(contractId, Interceptor.composite(interceptors));
    }

    public final class Delegate<C> {
        Delegate<Object> nextDelegate = null;
        private final ContractId<C> contractId;
        private final Interceptor interceptor;
        public final C proxy;
        private volatile C implementation;
        @SuppressWarnings("unchecked") Delegate(final ContractId<C> contractId, final Interceptor interceptor) {
            nextDelegate = firstDelegate;
            firstDelegate = (Delegate<Object>)this;
            this.contractId = contractId;
            this.interceptor = interceptor;
            proxy = (C)Proxy.newProxyInstance(
                contractId.contract.getClassLoader(),
                new Class<?>[] {contractId.contract},
                (proxy, method, arguments) -> {
                    final C implementation = this.implementation;
                    if (implementation == null) {
                        throw new SessionClosedException();
                    }
                    try {
                        return method.invoke(implementation, arguments);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            );
        }
        void setImplementation(final Session session) {
            implementation = session.proxy(contractId, interceptor);
        }
    }

    /**
     * @see Session#opened()
     */
    protected void opened() throws Exception {
        // empty
    }

    /**
     * @see Session#closed(Throwable)
     */
    protected abstract void closed(@Nullable Throwable throwable) throws Exception;

    public final class Session extends ch.softappeal.yass.core.remote.session.Session {
        public final Link link = Link.this;
        Session(final SessionClient sessionClient) {
            super(sessionClient);
        }
        @Override protected void opened() throws Exception {
            session = this;
            forEachDelegate(delegate -> delegate.setImplementation(this));
            Link.this.opened();
        }
        @Override protected void closed(@Nullable final Throwable throwable) throws Exception {
            session = null;
            Link.this.closed(throwable);
        }
    }

    public final SessionFactory sessionFactory = Session::new;

    @Nullable private volatile Session session = null;

    @Nullable public final Session session() {
        return session;
    }

    public final boolean up() {
        return (session != null);
    }

}
