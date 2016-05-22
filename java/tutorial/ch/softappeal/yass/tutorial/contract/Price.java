package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

@Tag(11) public final class Price {

    @Tag(1) public final int instrumentId;
    @Tag(2) public final int value;
    @Tag(3) public final PriceKind kind;

    public Price(final int instrumentId, final int value, final PriceKind kind) {
        this.instrumentId = instrumentId;
        this.value = value;
        this.kind = Check.notNull(kind);
    }

}
