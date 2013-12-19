package ch.softappeal.yass.tutorial.session.contract.instrument;

import ch.softappeal.yass.tutorial.session.contract.Instrument;
import ch.softappeal.yass.util.Tag;

@Tag(11) public final class Bond extends Instrument {

  @Tag(10) public final double coupon;

  public Bond(final String id, final String name, final double coupon) {
    super(id, name);
    this.coupon = coupon;
  }

}
