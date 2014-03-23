package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;

/**
 * Combines a contract with an id.
 * Factory for {@link Service} and {@link Invoker}.
 * @param <C> the contract type
 */
public final class ContractId<C> {

  public final Class<C> contract;
  public final Object id;

  private ContractId(final Class<C> contract, final Object id) {
    this.contract = Check.notNull(contract);
    this.id = Check.notNull(id);
  }

  public static <C> ContractId<C> create(final Class<C> contract, final Object id) {
    return new ContractId<C>(contract, id);
  }

}
