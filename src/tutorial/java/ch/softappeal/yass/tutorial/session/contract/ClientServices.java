package ch.softappeal.yass.tutorial.session.contract;

import ch.softappeal.yass.core.remote.ContractId;

/**
 * Services to be implemented by client.
 */
public final class ClientServices {

  public static final ContractId<PriceListener> PriceListener = ContractId.create(PriceListener.class, 0);

}
