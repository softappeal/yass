package ch.softappeal.yass.tutorial.initiator;

import ch.softappeal.yass.remote.session.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.contract.instrument.*;

/**
 * The proxies of this class survive a session reconnect.
 */
public final class InitiatorReconnector extends Reconnector<InitiatorSession> {

    public final PriceEngine priceEngine = proxy(PriceEngine.class, session -> session.priceEngine);
    public final InstrumentService instrumentService =
        proxy(InstrumentService.class, session -> session.instrumentService);
    public final EchoService echoService = proxy(EchoService.class, session -> session.echoService);

}
