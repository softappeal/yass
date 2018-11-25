package ch.softappeal.yass.tutorial.contract;

import java.util.List;
import java.util.Objects;

public final class UnknownInstrumentsException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public final List<Integer> instrumentIds;

    public Object onlyNeededForTests1;

    public byte[] onlyNeededForTests2;

    public Throwable onlyNeededForTests3;

    public UnknownInstrumentsException(final List<Integer> instrumentIds) {
        this.instrumentIds = Objects.requireNonNull(instrumentIds);
    }

    @Override
    public String getMessage() {
        return "there are " + instrumentIds.size() + " unknown instruments";
    }

}
