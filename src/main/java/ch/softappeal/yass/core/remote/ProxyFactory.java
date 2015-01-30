package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;

@FunctionalInterface public interface ProxyFactory {

    /**
     * @return a proxy for contractId using interceptors
     */
    <C> C proxy(ContractId<C> contractId, Interceptor... interceptors);

}
