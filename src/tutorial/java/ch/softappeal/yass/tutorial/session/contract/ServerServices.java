package ch.softappeal.yass.tutorial.session.contract;

import ch.softappeal.yass.core.remote.ContractId;

/**
 * Services to be implemented by server.
 */
public final class ServerServices {

  public static final ContractId<PriceEngine> PriceEngine = ContractId.create(PriceEngine.class, 0);
  public static final ContractId<InstrumentService> InstrumentService = ContractId.create(InstrumentService.class, 1);

}
