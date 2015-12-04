package ch.softappeal.yass.tutorial.acceptor;

import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.util.Check;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class PriceEngineImpl implements PriceEngine {

    private final Map<Integer, Instrument> instruments;
    private final Supplier<Set<Integer>> subscribedInstrumentIds;

    public PriceEngineImpl(final Map<Integer, Instrument> instruments, final Supplier<Set<Integer>> subscribedInstrumentIds) {
        this.instruments = Check.notNull(instruments);
        this.subscribedInstrumentIds = Check.notNull(subscribedInstrumentIds);
    }

    @Override public void subscribe(final List<Integer> instrumentIds) throws UnknownInstrumentsException {
        final Set<Integer> unknownInstrumentIds = new HashSet<>();
        for (final int instrumentId : instrumentIds) {
            if (instruments.containsKey(instrumentId)) {
                subscribedInstrumentIds.get().add(instrumentId); // shows how to do session safe dependency injection
            } else {
                unknownInstrumentIds.add(instrumentId);
            }
        }
        if (!unknownInstrumentIds.isEmpty()) {
            throw new UnknownInstrumentsException(new ArrayList<>(unknownInstrumentIds));
        }
    }

}
