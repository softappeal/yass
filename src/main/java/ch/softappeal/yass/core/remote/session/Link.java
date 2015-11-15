package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Wraps a client side {@link ch.softappeal.yass.core.remote.session.Session}.
 * @see Reconnector
 */
public abstract class Link {

    private @Nullable Delegate<Object> firstDelegate;

    /**
     * @return a proxy surviving {@link ch.softappeal.yass.core.remote.session.Session#closed(Throwable)}
     */
    protected final <C> C proxy(final ContractId<C> contractId, final Interceptor... interceptors) {
        return new Delegate<>(contractId, Interceptors.composite(interceptors)).proxy;
    }

    private final class Delegate<C> {
        final @Nullable Delegate<Object> nextDelegate;
        private final ContractId<C> contractId;
        private final Interceptor interceptor;
        final C proxy;
        private volatile C implementation;
        @SuppressWarnings("unchecked") Delegate(final ContractId<C> contractId, final Interceptor interceptor) {
            nextDelegate = firstDelegate;
            firstDelegate = (Delegate<Object>)this;
            this.contractId = contractId;
            this.interceptor = interceptor;
            proxy = (C)Proxy.newProxyInstance(
                contractId.contract.getClassLoader(),
                new Class<?>[] {contractId.contract},
                new InvocationHandler() {
                    @Override public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                        final C implementation = Delegate.this.implementation;
                        if (implementation == null) {
                            throw new SessionClosedException();
                        }
                        try {
                            return method.invoke(implementation, arguments);
                        } catch (final InvocationTargetException e) {
                            throw e.getCause();
                        }
                    }
                }
            );
        }
        void setImplementation(final Session session) {
            implementation = session.proxy(contractId, interceptor);
        }
    }

    /**
     * @see ch.softappeal.yass.core.remote.session.Session#opened()
     */
    protected void opened() throws Exception {
        // empty
    }

    /**
     * @see ch.softappeal.yass.core.remote.session.Session#closed(Throwable)
     */
    protected abstract void closed(@Nullable Throwable throwable) throws Exception;

    public final class Session extends ch.softappeal.yass.core.remote.session.Session {
        public final Link link = Link.this;
        Session(final SessionClient sessionClient) {
            super(sessionClient);
        }
        @Override protected void opened() throws Exception {
            session = this;
            for (Delegate<Object> delegate = firstDelegate; delegate != null; delegate = delegate.nextDelegate) {
                delegate.setImplementation(this);
            }
            Link.this.opened();
        }
        @Override protected void closed(final @Nullable Throwable throwable) throws Exception {
            session = null;
            Link.this.closed(throwable);
        }
    }

    public final SessionFactory sessionFactory = new SessionFactory() {
        @Override public ch.softappeal.yass.core.remote.session.Session create(final SessionClient sessionClient) throws Exception {
            return new Session(sessionClient);
        }
    };

    private volatile @Nullable Session session = null;

    public final @Nullable Session session() {
        return session;
    }

    public final boolean up() {
        return (session != null);
    }

}
