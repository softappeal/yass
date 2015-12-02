package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.ContractId;

/**
 * Services to be implemented by initiator.
 */
public final class InitiatorServices {

    public static final ContractId<PriceListener> PriceListener = ContractId.create(PriceListener.class, 0);
    public static final ContractId<EchoService> EchoService = ContractId.create(EchoService.class, 1);

}
