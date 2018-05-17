package ch.softappeal.yass.tutorial.contract.instrument.stock;

import ch.softappeal.yass.tutorial.contract.Instrument;

/**
 * Shows deep nesting.
 */
public final class Stock extends Instrument {

    public final Boolean paysDividend;

    public Stock(final int id, final String name, final Boolean paysDividend) {
        super(id, name);
        this.paysDividend = paysDividend;
    }

}
