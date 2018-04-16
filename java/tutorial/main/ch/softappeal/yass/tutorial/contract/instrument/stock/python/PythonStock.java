package ch.softappeal.yass.tutorial.contract.instrument.stock.python;

import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;

/**
 * Needed for testing module import in Python.
 */
public final class PythonStock extends Stock {

    public PythonStock(final int id, final String name, final @Nullable Boolean paysDividend) {
        super(id, name, paysDividend);
    }

}
