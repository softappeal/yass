package ch.softappeal.yass.tutorial.initiator;

import ch.softappeal.yass.core.remote.session.Reconnector;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;

/**
 * The proxies of this class survive a session reconnect.
 */
public final class InitiatorReconnector extends Reconnector<InitiatorSession> {

    public final PriceEngine priceEngine = proxy(PriceEngine.class, new SessionProxyGetter<InitiatorSession, PriceEngine>() {
        @Override public PriceEngine get(final InitiatorSession session) throws Exception {
            return session.priceEngine;
        }
    });

    public final InstrumentService instrumentService = proxy(InstrumentService.class, new SessionProxyGetter<InitiatorSession, InstrumentService>() {
        @Override public InstrumentService get(final InitiatorSession session) throws Exception {
            return session.instrumentService;
        }
    });

    public final EchoService echoService = proxy(EchoService.class, new SessionProxyGetter<InitiatorSession, EchoService>() {
        @Override public EchoService get(final InitiatorSession session) throws Exception {
            return session.echoService;
        }
    });

}
