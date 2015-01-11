package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Check;

public final class Price {

    public final int instrumentId;
    public final int value;
    public final PriceType type;

    public Price(final int instrumentId, final int value, final PriceType type) {
        this.instrumentId = instrumentId;
        this.value = value;
        this.type = Check.notNull(type);
    }

}
