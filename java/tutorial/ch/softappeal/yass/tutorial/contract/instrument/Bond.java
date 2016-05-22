package ch.softappeal.yass.tutorial.contract.instrument;

import ch.softappeal.yass.tutorial.contract.Expiration;
import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

@Tag(13) public final class Bond extends Instrument {

    @Tag(3) public final double coupon;
    @Tag(4) public final Expiration expiration;

    public Bond(final int id, final String name, final double coupon, final Expiration expiration) {
        super(id, name);
        this.coupon = coupon;
        this.expiration = Check.notNull(expiration);
    }

}
