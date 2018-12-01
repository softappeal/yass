package ch.softappeal.yass.tutorial.contract.instrument;

import ch.softappeal.yass.tutorial.contract.*;

import java.util.*;

public final class Bond extends Instrument {

    public final double coupon;
    public final Expiration expiration;

    public Bond(final int id, final String name, final double coupon, final Expiration expiration) {
        super(id, name);
        this.coupon = coupon;
        this.expiration = Objects.requireNonNull(expiration);
    }

}
