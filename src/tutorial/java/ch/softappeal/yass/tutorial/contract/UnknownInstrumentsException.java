package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.util.Check;

import java.util.List;

public final class UnknownInstrumentsException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public final List<Integer> instrumentIds;

    public UnknownInstrumentsException(final List<Integer> instrumentIds) {
        this.instrumentIds = Check.notNull(instrumentIds);
    }

    /**
     * @see AbstractFastSerializer
     */
    @Override public String getMessage() {
        return "there are " + instrumentIds.size() + " unknown instruments";
    }

}
