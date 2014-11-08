package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.util.List;

public final class UnknownInstrumentsException extends Exception {

    private static final long serialVersionUID = 1L;

    public final List<String> instrumentIds;
    @Nullable public Object comment;
    @Nullable public byte[] dump;

    public UnknownInstrumentsException(final List<String> instrumentIds) {
        this.instrumentIds = Check.notNull(instrumentIds);
    }

    /**
     * @see AbstractFastSerializer
     */
    @Override public String getMessage() {
        return "there are " + instrumentIds.size() + " unknown instruments";
    }

}
