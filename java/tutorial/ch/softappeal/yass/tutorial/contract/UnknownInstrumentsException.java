package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.fast.FastSerializer;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.util.List;

public final class UnknownInstrumentsException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public final List<Integer> instrumentIds;

    public @Nullable Object onlyNeededForTests1;

    public @Nullable byte[] onlyNeededForTests2;

    public @Nullable Throwable onlyNeededForTests3;

    public UnknownInstrumentsException(final List<Integer> instrumentIds) {
        this.instrumentIds = Check.notNull(instrumentIds);
    }

    /**
     * @see FastSerializer
     */
    @Override public String getMessage() {
        return "there are " + instrumentIds.size() + " unknown instruments";
    }

}
