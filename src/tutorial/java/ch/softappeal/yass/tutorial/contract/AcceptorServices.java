package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;

/**
 * Services to be implemented by acceptor.
 */
public final class AcceptorServices {

    public static final ContractId<PriceEngine> PriceEngine = ContractId.create(PriceEngine.class, 0, Config.METHOD_MAPPER_FACTORY);
    public static final ContractId<InstrumentService> InstrumentService = ContractId.create(InstrumentService.class, 1, Config.METHOD_MAPPER_FACTORY);
    public static final ContractId<EchoService> EchoService = ContractId.create(EchoService.class, 2, Config.METHOD_MAPPER_FACTORY);

}
