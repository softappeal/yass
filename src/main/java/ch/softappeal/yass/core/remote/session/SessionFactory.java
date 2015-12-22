package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.ContractId;

@FunctionalInterface public interface SessionFactory {

    /**
     * It's allowed to call {@link Client#proxy(ContractId, Interceptor...)} during this method,
     * but the proxies can be used not before {@link Session#opened()} is called.
     * If this method throws an exception, the connection is rejected and {@link Session#closed(boolean)} won't be called.
     */
    Session create(Connection connection) throws Exception;

}
