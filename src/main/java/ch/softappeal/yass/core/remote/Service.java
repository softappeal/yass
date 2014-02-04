package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.util.Check;

/**
 * Server side service.
 */
public final class Service {

  final ContractId<?> contractId;
  final Object implementation;
  final Interceptor interceptor;

  /**
   * @see ContractId#service(Object, Interceptor...)
   */
  <C> Service(final ContractId<C> contractId, final C implementation, final Interceptor... interceptors) {
    this.contractId = Check.notNull(contractId);
    this.implementation = Check.notNull(implementation);
    interceptor = Interceptors.composite(interceptors);
  }

}
