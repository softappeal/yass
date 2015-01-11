package ch.softappeal.yass.tutorial.contract.instrument;

import ch.softappeal.yass.tutorial.contract.Expiration;
import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.instrument.stock.JsDouble;
import ch.softappeal.yass.util.Check;

public final class Bond extends Instrument {

    public final JsDouble coupon;
    public final Expiration expiration;

    public Bond(final int id, final String name, final double coupon, final Expiration expiration) {
        super(id, name);
        this.coupon = new JsDouble(coupon);
        this.expiration = Check.notNull(expiration);
    }

}
