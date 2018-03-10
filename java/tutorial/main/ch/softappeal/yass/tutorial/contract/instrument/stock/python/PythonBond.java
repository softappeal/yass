package ch.softappeal.yass.tutorial.contract.instrument.stock.python;

import ch.softappeal.yass.tutorial.contract.Expiration;
import ch.softappeal.yass.tutorial.contract.instrument.Bond;

/**
 * Needed for testing module import in Python.
 */
public final class PythonBond extends Bond {

    public PythonBond(final int id, final String name, final double coupon, final Expiration expiration) {
        super(id, name, coupon, expiration);
    }

}
