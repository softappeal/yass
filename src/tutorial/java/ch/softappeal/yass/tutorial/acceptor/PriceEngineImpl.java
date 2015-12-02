package ch.softappeal.yass.tutorial.acceptor;

import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.ContextLocator;
import ch.softappeal.yass.util.ContextService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PriceEngineImpl extends ContextService<PriceEngineContext> implements PriceEngine {

    private final Map<Integer, Instrument> instruments;

    public PriceEngineImpl(final Map<Integer, Instrument> instruments, final ContextLocator<PriceEngineContext> locator) {
        super(locator);
        this.instruments = Check.notNull(instruments);
    }

    @Override public void subscribe(final List<Integer> instrumentIds) throws UnknownInstrumentsException {
        final Set<Integer> subscribedInstrumentIds = context().subscribedInstrumentIds();
        final Set<Integer> unknownInstrumentIds = new HashSet<>();
        for (final int instrumentId : instrumentIds) {
            if (instruments.containsKey(instrumentId)) {
                subscribedInstrumentIds.add(instrumentId);
            } else {
                unknownInstrumentIds.add(instrumentId);
            }
        }
        if (!unknownInstrumentIds.isEmpty()) {
            throw new UnknownInstrumentsException(new ArrayList<>(unknownInstrumentIds));
        }
    }

}
