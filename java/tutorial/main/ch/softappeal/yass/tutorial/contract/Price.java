package ch.softappeal.yass.tutorial.contract;

import java.util.Objects;

public final class Price {

    public final int instrumentId;
    public final int value;
    public final PriceKind kind;

    public Price(final int instrumentId, final int value, final PriceKind kind) {
        this.instrumentId = instrumentId;
        this.value = value;
        this.kind = Objects.requireNonNull(kind);
    }

}
