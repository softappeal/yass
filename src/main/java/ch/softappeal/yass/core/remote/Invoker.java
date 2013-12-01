package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;

/**
 * Client side factory for proxies that invoke a remote service.
 * @param <C> the contract type
 */
public interface Invoker<C> {

  /**
   * @return a proxy for the contract using interceptors
   */
  C proxy(Interceptor... interceptors);

}
