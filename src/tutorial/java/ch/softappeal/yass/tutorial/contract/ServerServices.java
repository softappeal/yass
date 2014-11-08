package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;

/**
 * Services to be implemented by server.
 */
public final class ServerServices {

    public static final ContractId<PriceEngine> PriceEngine = ContractId.create(PriceEngine.class, 0);
    public static final ContractId<InstrumentService> InstrumentService = ContractId.create(InstrumentService.class, 1);
    public static final ContractId<EchoService> EchoService = ContractId.create(EchoService.class, 2);

}
