package ch.softappeal.yass.tutorial.initiator;

import ch.softappeal.yass.remote.session.Reconnector;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;

/**
 * The proxies of this class survive a session reconnect.
 */
public final class InitiatorReconnector extends Reconnector<InitiatorSession> {

    public final PriceEngine priceEngine = proxy(PriceEngine.class, session -> session.priceEngine);
    public final InstrumentService instrumentService = proxy(InstrumentService.class, session -> session.instrumentService);
    public final EchoService echoService = proxy(EchoService.class, session -> session.echoService);

}
