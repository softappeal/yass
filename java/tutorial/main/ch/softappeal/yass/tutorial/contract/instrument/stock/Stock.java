package ch.softappeal.yass.tutorial.contract.instrument.stock;

import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.util.Nullable;

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
