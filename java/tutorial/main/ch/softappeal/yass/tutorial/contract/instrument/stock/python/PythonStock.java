package ch.softappeal.yass.tutorial.contract.instrument.stock.python;

import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;

/**
 * Needed for testing module import in Python.
 */
public final class PythonStock extends Stock {

    public PythonStock(final int id, final String name, final Boolean paysDividend) {
        super(id, name, paysDividend);
    }

}
