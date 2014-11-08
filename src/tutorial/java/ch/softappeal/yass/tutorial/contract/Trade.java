package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Check;

public final class Trade {

    public final Instrument instrument;
    public final int amount;

    public Trade(final Instrument instrument, final int amount) {
        this.instrument = Check.notNull(instrument);
        this.amount = amount;
    }

}
