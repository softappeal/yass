package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

import java.util.List;

@Tag(15) public final class UnknownInstrumentsException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    @Tag(1) public final List<Integer> instrumentIds;

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
