package ch.softappeal.yass.tutorial.contract.instrument.stock;

import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;

/**
 * Shows deep nesting.
 */
@Tag(12) public final class Stock extends Instrument {

    @Tag(3) public final @Nullable Boolean paysDividend;

    public Stock(final int id, final String name, final Boolean paysDividend) {
        super(id, name);
        this.paysDividend = paysDividend;
    }

}
