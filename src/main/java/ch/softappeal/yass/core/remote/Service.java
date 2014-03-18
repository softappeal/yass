package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.util.Check;

public final class Service {

  final ContractId<?> contractId;
  final Object implementation;
  final Interceptor interceptor;

  /**
   * Note: It's a good idea to add an interceptor that handles unexpected exceptions
   * (this is especially useful for oneway methods where these are ignored and NOT passed to the client).
   */
  public <C> Service(final ContractId<C> contractId, final C implementation, final Interceptor... interceptors) {
    this.contractId = Check.notNull(contractId);
    this.implementation = Check.notNull(implementation);
    interceptor = Interceptors.composite(interceptors);
  }

}
