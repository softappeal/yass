package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Instantiators;
import ch.softappeal.yass.util.unsupported.UnsupportedInstantiators;

import java.util.Objects;

public final class Price {

    public final int instrumentId;
    public final int value;
    public final PriceKind kind;

    /**
     * Needed for {@link Instantiators#NOARG}.
     * Not needed for {@link UnsupportedInstantiators#UNSAFE}.
     */
    private Price() {
        instrumentId = 0;
        value = 0;
        kind = null;
    }

    public Price(final int instrumentId, final int value, final PriceKind kind) {
        this.instrumentId = instrumentId;
        this.value = value;
        this.kind = Objects.requireNonNull(kind);
    }

}
