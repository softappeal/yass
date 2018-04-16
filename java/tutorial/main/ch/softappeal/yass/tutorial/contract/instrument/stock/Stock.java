package ch.softappeal.yass.tutorial.contract.instrument.stock;

import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.tutorial.contract.Instrument;

/**
 * Shows deep nesting.
 */
public class Stock extends Instrument {

    public final @Nullable Boolean paysDividend;

    public Stock(final int id, final String name, final @Nullable Boolean paysDividend) {
        super(id, name);
        this.paysDividend = paysDividend;
    }

}
