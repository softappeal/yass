package ch.softappeal.yass.tutorial.acceptor;

import ch.softappeal.yass.tutorial.contract.*;

import java.util.*;

public final class PriceEngineImpl implements PriceEngine {

    private final Set<Integer> subscribedInstrumentIds;

    public PriceEngineImpl(final Set<Integer> subscribedInstrumentIds) {
        this.subscribedInstrumentIds = Objects.requireNonNull(subscribedInstrumentIds);
    }

    @Override
    public void subscribe(final List<Integer> instrumentIds) throws UnknownInstrumentsException {
        final Set<Integer> unknownInstrumentIds = new HashSet<>();
        for (final int instrumentId : instrumentIds) {
            if (InstrumentServiceImpl.INSTRUMENTS.containsKey(instrumentId)) {
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
