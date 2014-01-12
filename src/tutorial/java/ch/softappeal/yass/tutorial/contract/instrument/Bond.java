package ch.softappeal.yass.tutorial.contract.instrument;

import ch.softappeal.yass.tutorial.contract.Instrument;

public final class Bond extends Instrument {

  public final int coupon;

  public Bond(final String id, final String name, final int coupon) {
    super(id, name);
    this.coupon = coupon;
  }

}
