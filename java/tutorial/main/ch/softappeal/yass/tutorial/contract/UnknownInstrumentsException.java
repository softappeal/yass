package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.serialize.fast.FastSerializer;

import java.util.List;
import java.util.Objects;

public final class UnknownInstrumentsException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public final List<Integer> instrumentIds;

    public @Nullable Object onlyNeededForTests1;

    public @Nullable byte[] onlyNeededForTests2;

    public @Nullable Throwable onlyNeededForTests3;

    public UnknownInstrumentsException(final List<Integer> instrumentIds) {
        this.instrumentIds = Objects.requireNonNull(instrumentIds);
    }

    /**
     * @see FastSerializer
     */
    @Override public String getMessage() {
        return "there are " + instrumentIds.size() + " unknown instruments";
    }

}
